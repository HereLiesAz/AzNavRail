package com.hereliesaz.aznavrail.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import java.io.OutputStreamWriter

/**
 * Generates an `AzGraph` object for every class annotated with `@Az`.
 *
 * `AzActivity` has always required `abstract val graph: AzGraphInterface`, described in the docs as
 * "the generated `AzGraph` object" — but nothing generated one, so the whole High-Inference API was
 * unusable. This is the generator that makes it real.
 *
 * The output is deliberately plain: it emits the same DSL a developer would write by hand, so the
 * generated file is readable, debuggable, and carries no runtime of its own. Anything the
 * annotations cannot express goes in `AzActivity.configureRail()`, which the generated graph calls
 * inside the same DSL block.
 */
class AzSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    private var handled = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // KSP calls `process` in rounds; everything here is resolved in the first one.
        if (handled) return emptyList()
        handled = true

        val annotated = resolver.getSymbolsWithAnnotation(AZ_ANNOTATION).toList()
        val hostClasses = annotated.filterIsInstance<KSClassDeclaration>()
        if (hostClasses.isEmpty()) return emptyList()

        hostClasses.forEach { host ->
            runCatching { generate(host) }
                .onFailure { logger.error("AzNavRail: failed to generate a graph for ${host.simpleName.asString()}: ${it.message}", host) }
        }
        return emptyList()
    }

    private fun generate(host: KSClassDeclaration) {
        val pkg = host.packageName.asString()
        val name = host.simpleName.asString()
        val graphName = "${name}AzGraph"

        val classAz = host.annotationOf(AZ_SIMPLE) ?: return
        val app = classAz.nested("app")
        val advanced = classAz.nested("advanced")

        // Items come from the annotated members, in declaration order — which is the order they will
        // appear in the rail, matching how the hand-written DSL behaves.
        val items = buildList {
            host.declarations.forEach { decl ->
                val az = decl.annotationOf(AZ_SIMPLE) ?: return@forEach
                renderItem(decl, az)?.let { add(it) }
            }
        }

        val startDestination = app?.string("startDestination").orEmpty()

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = false, host.containingFile!!),
            packageName = pkg,
            fileName = graphName,
        )
        OutputStreamWriter(file, Charsets.UTF_8).use { out ->
            out.write(
                buildGraph(
                    pkg = pkg,
                    hostName = name,
                    graphName = graphName,
                    configLines = configLines(app, advanced),
                    itemLines = items,
                    startDestination = startDestination,
                )
            )
        }
    }

    private fun configLines(app: KSAnnotation?, advanced: KSAnnotation?): List<String> = buildList {
        if (app != null) {
            val side = app.enumName("dockingSide") ?: "LEFT"
            add(
                "azConfig(" +
                    "dockingSide = AzDockingSide.$side, " +
                    "packButtons = ${app.boolean("packRailButtons")}, " +
                    "vibrate = ${app.boolean("vibrate")}, " +
                    "displayAppName = ${app.boolean("displayAppName")})"
            )
        }
        if (advanced != null) {
            val loading = advanced.string("isLoadingProperty").orEmpty()
            add(
                "azAdvanced(" +
                    (if (loading.isNotBlank()) "isLoading = instance.$loading, " else "") +
                    "helpEnabled = ${advanced.boolean("helpEnabled")}, " +
                    "enableRailDragging = ${advanced.boolean("enableRailDragging")}, " +
                    "autoGuidanceEdges = ${advanced.boolean("autoGuidanceEdges")})"
            )
        }
    }

    /** One DSL call for one annotated member, or null when the `@Az` declared nothing renderable. */
    private fun renderItem(decl: KSDeclaration, az: KSAnnotation): String? {
        val member = decl.simpleName.asString()
        val isFunction = decl is KSFunctionDeclaration
        val isProperty = decl is KSPropertyDeclaration
        // A function is the item's action; a property is its text. Both are addressed through
        // `instance`, the Activity cast back to its own type.
        val click = if (isFunction) " { instance.$member() }" else " { }"

        az.nested("rail")?.takeIf { it.string("id").orEmpty().isNotBlank() }?.let { a ->
            return itemCall("azRailItem", a, member, isProperty, click) + stateCall(a)
        }
        az.nested("menu")?.takeIf { it.string("id").orEmpty().isNotBlank() }?.let { a ->
            return itemCall("azMenuItem", a, member, isProperty, click) + stateCall(a)
        }
        az.nested("host")?.takeIf { it.string("id").orEmpty().isNotBlank() }?.let { a ->
            val id = a.string("id")!!
            return "azRailHostItem(id = ${id.q()}, text = ${textArg(a, member, isProperty)}" +
                optional("route", a.string("route")) +
                optional("screenTitle", a.string("screenTitle")) +
                optional("info", a.string("info")) +
                ", initiallyExpanded = ${a.boolean("initiallyExpanded")})$click"
        }
        az.nested("sub")?.takeIf { it.string("id").orEmpty().isNotBlank() }?.let { a ->
            val id = a.string("id")!!
            val hostId = a.string("hostId").orEmpty()
            return "azRailSubItem(id = ${id.q()}, hostId = ${hostId.q()}, text = ${textArg(a, member, isProperty)}" +
                optional("route", a.string("route")) +
                optional("info", a.string("info")) + ")$click"
        }
        az.nested("toggle")?.takeIf { it.string("id").orEmpty().isNotBlank() }?.let { a ->
            val id = a.string("id")!!
            val prop = a.string("isCheckedProperty").orEmpty()
            if (prop.isBlank()) {
                logger.error("AzNavRail: Toggle '$id' needs an isCheckedProperty — it is what the generated onClick flips.", decl)
                return null
            }
            val fn = if (a.boolean("rail")) "azRailToggle" else "azMenuToggle"
            // The generated click flips the bound property, then runs the member if it is a function.
            val extra = if (isFunction) "; instance.$member()" else ""
            return "$fn(id = ${id.q()}, isChecked = instance.$prop, " +
                "toggleOnText = ${a.string("toggleOnText").orEmpty().q()}, " +
                "toggleOffText = ${a.string("toggleOffText").orEmpty().q()}" +
                optional("info", a.string("info")) +
                ") { instance.$prop = !instance.$prop$extra }"
        }
        az.nested("cycler")?.takeIf { it.string("id").orEmpty().isNotBlank() }?.let { a ->
            val id = a.string("id")!!
            val selected = a.string("selectedOptionProperty").orEmpty()
            if (selected.isBlank()) {
                logger.error("AzNavRail: Cycler '$id' needs a selectedOptionProperty — it is what the generated onClick advances.", decl)
                return null
            }
            val optionsProp = a.string("optionsProperty").orEmpty()
            val optionsExpr = if (optionsProp.isNotBlank()) "instance.$optionsProp"
            else a.strings("options").joinToString(prefix = "listOf(", postfix = ")") { it.q() }
            val fn = if (a.boolean("rail")) "azRailCycler" else "azMenuCycler"
            val extra = if (isFunction) "; instance.$member()" else ""
            return "$fn(id = ${id.q()}, options = $optionsExpr, selectedOption = instance.$selected" +
                optional("info", a.string("info")) +
                ") { val opts = $optionsExpr; " +
                "instance.$selected = opts[(opts.indexOf(instance.$selected) + 1).mod(opts.size)]$extra }"
        }
        az.nested("divider")?.takeIf { it.boolean("enabled") }?.let { return "azDivider()" }
        return null
    }

    private fun itemCall(fn: String, a: KSAnnotation, member: String, isProperty: Boolean, click: String): String {
        val id = a.string("id")!!
        return "$fn(id = ${id.q()}, text = ${textArg(a, member, isProperty)}" +
            optional("route", a.string("route")) +
            optional("screenTitle", a.string("screenTitle")) +
            optional("info", a.string("info")) +
            (a.string("disabledProperty").orEmpty().takeIf { it.isNotBlank() }?.let { ", disabled = instance.$it" } ?: "") +
            ")$click"
    }

    /**
     * The reactive per-item state (`badgeProperty` / `loadingProperty`) is emitted as a separate
     * `azItemState` call rather than more arguments, because that is the uniform channel the DSL
     * exposes and it works identically for every item kind.
     */
    private fun stateCall(a: KSAnnotation): String {
        val badge = a.string("badgeProperty").orEmpty()
        val loading = a.string("loadingProperty").orEmpty()
        if (badge.isBlank() && loading.isBlank()) return ""
        val id = a.string("id")!!
        val args = buildList {
            if (badge.isNotBlank()) add("badge = instance.$badge?.toString()")
            if (loading.isNotBlank()) add("isLoading = instance.$loading")
        }
        return "\n        azItemState(id = ${id.q()}, ${args.joinToString(", ")})"
    }

    private fun textArg(a: KSAnnotation, member: String, isProperty: Boolean): String {
        val bound = a.string("textProperty").orEmpty()
        if (bound.isNotBlank()) return "instance.$bound"
        val literal = a.string("text").orEmpty()
        if (literal.isNotBlank()) return literal.q()
        // A property with no explicit text supplies its own value; a function falls back to its name.
        return if (isProperty) "instance.$member" else member.q()
    }

    private fun optional(name: String, value: String?): String =
        if (value.isNullOrBlank()) "" else ", $name = ${value.q()}"

    private fun buildGraph(
        pkg: String,
        hostName: String,
        graphName: String,
        configLines: List<String>,
        itemLines: List<String>,
        startDestination: String,
    ): String {
        val body = (configLines + itemLines).joinToString("\n") { "        $it" }
        val navHost = if (startDestination.isBlank()) "" else """
            onscreen(alignment = Alignment.Center) {
                AzNavHost(startDestination = ${startDestination.q()}, navController = navController) {
                    instance.azGraphDestinations(this)
                }
            }
"""
        return """// Generated by the AzNavRail KSP processor. Do not edit.
package $pkg

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.Alignment
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzGraphInterface
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.AzNavHost
import com.hereliesaz.aznavrail.model.AzDockingSide

/**
 * The navigation graph declared by [$hostName]'s `@Az` annotations.
 *
 * This is exactly the DSL you would have written by hand; nothing here is magic at runtime.
 * Assign it to `$hostName.graph`.
 */
public object $graphName : AzGraphInterface {
    override fun run(activity: ComponentActivity) {
        val instance = activity as $hostName
        activity.setContent {
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
$body
$navHost
                // Anything the annotations can't express.
                with(instance) { configureRail() }
            }
        }
    }
}
"""
    }

    // --- KSP annotation reading -------------------------------------------------------------

    private fun KSAnnotated.annotationOf(simpleName: String): KSAnnotation? =
        annotations.firstOrNull { it.shortName.asString() == simpleName }

    private fun KSAnnotation.nested(argument: String): KSAnnotation? =
        arguments.firstOrNull { it.name?.asString() == argument }?.value as? KSAnnotation

    private fun KSAnnotation.string(argument: String): String? =
        arguments.firstOrNull { it.name?.asString() == argument }?.value as? String

    @Suppress("UNCHECKED_CAST")
    private fun KSAnnotation.strings(argument: String): List<String> =
        (arguments.firstOrNull { it.name?.asString() == argument }?.value as? List<String>).orEmpty()

    private fun KSAnnotation.boolean(argument: String): Boolean =
        arguments.firstOrNull { it.name?.asString() == argument }?.value as? Boolean ?: false

    /** Enum arguments arrive as a `KSType`/`KSClassDeclaration` depending on KSP version; take the name. */
    private fun KSAnnotation.enumName(argument: String): String? {
        val raw = arguments.firstOrNull { it.name?.asString() == argument }?.value ?: return null
        return raw.toString().substringAfterLast('.').takeIf { it.isNotBlank() }
    }

    private fun String.q(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private companion object {
        const val AZ_ANNOTATION = "com.hereliesaz.aznavrail.annotation.Az"
        const val AZ_SIMPLE = "Az"
    }
}

/** KSP entry point. Registered via `META-INF/services`. */
class AzSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        AzSymbolProcessor(environment.codeGenerator, environment.logger)
}
