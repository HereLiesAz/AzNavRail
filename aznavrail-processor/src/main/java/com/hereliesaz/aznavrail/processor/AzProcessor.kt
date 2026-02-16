package com.hereliesaz.aznavrail.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import java.util.Locale

class AzProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private var isGenerated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (isGenerated) return emptyList()

        val symbols = resolver.getSymbolsWithAnnotation("com.hereliesaz.aznavrail.annotation.Az").toList()

        if (symbols.isEmpty()) return emptyList()

        val invalidSymbols = symbols.filter { !it.validate() }
        if (invalidSymbols.isNotEmpty()) {
            return symbols
        }

        val activityClass = symbols.filterIsInstance<KSClassDeclaration>().firstOrNull()

        if (activityClass == null) {
            return emptyList()
        }

        val packageName = activityClass.packageName.asString()
        val graphClassName = "AzGraph"

        val fileSpec = FileSpec.builder(packageName, graphClassName)
            .addImport("androidx.compose.runtime", "Composable")
            .addImport("androidx.activity.compose", "setContent")
            .addImport("androidx.navigation.compose", "rememberNavController", "composable")
            .addImport("com.hereliesaz.aznavrail", "AzHostActivityLayout", "AzNavHost", "AzGraphInterface")
            .addImport("com.hereliesaz.aznavrail.model", "AzDockingSide", "AzOrientation", "AzNestedRailAlignment")
            .addImport("androidx.activity", "ComponentActivity")

        val graphObject = TypeSpec.objectBuilder(graphClassName)
            .addSuperinterface(ClassName("com.hereliesaz.aznavrail", "AzGraphInterface"))

        val runFunction = FunSpec.builder("Run")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("activity", ClassName("androidx.activity", "ComponentActivity"))
            .addCode(generateRunBody(activityClass, symbols))

        graphObject.addFunction(runFunction.build())
        fileSpec.addType(graphObject.build())

        fileSpec.build().writeTo(codeGenerator, Dependencies(true, *symbols.mapNotNull { (it as? KSDeclaration)?.containingFile }.toTypedArray()))

        isGenerated = true
        return emptyList()
    }

    private fun generateRunBody(activityClass: KSClassDeclaration, symbols: List<KSAnnotated>): CodeBlock {
        val appConfig = extractAppConfig(activityClass)
        val items = extractItems(symbols, activityClass)

        val builder = CodeBlock.builder()

        builder.beginControlFlow("activity.setContent")
        builder.addStatement("val navController = rememberNavController()")

        builder.add("AzHostActivityLayout(\n")
        builder.indent()
        builder.addStatement("navController = navController,")
        builder.unindent()
        builder.beginControlFlow(")")

        // Generate azConfig call
        if (appConfig.dock != null || appConfig.orientation != null) {
            builder.add("azConfig(\n")
            builder.indent()
            if (appConfig.dock != null) builder.addStatement("dockingSide = %T.%L,", ClassName("com.hereliesaz.aznavrail.model", "AzDockingSide"), appConfig.dock)
            // Orientation isn't directly in azConfig in new API?
            // azConfig takes dockingSide, packButtons, noMenu, etc.
            // Orientation seems to be derived or passed elsewhere?
            // AzNavRail takes orientation. AzHostActivityLayout calculates it.
            // It seems 'orientation' is no longer directly configurable via azConfig?
            // Wait, AzApp annotation has orientation.
            // But AzNavRailScope.azConfig does NOT have orientation!
            // It seems orientation is inferred from docking side and rotation in AzHostActivityLayout.
            // So we might ignore orientation from annotation for now or log warning.
            builder.unindent()
            builder.addStatement(")\n")
        }

        // Separate items into top-level and children
        val topLevelItems = mutableListOf<ItemData>()
        val children = mutableMapOf<String, MutableList<ItemData>>()

        items.forEach { item ->
            if (item is NestedRailData && item.parent.isNotEmpty()) {
                children.getOrPut(item.parent) { mutableListOf() }.add(item)
            } else {
                topLevelItems.add(item)
            }
        }

        topLevelItems.forEach { item ->
            generateItem(builder, item, children)
        }

        builder.addStatement("")
        val contentItems = items.filter { it.hasContent }
        val startDest = if (contentItems.isNotEmpty()) {
            contentItems.first().id
        } else {
            "home"
        }

        builder.beginControlFlow("onscreen")
        builder.beginControlFlow("AzNavHost(startDestination = %S)", startDest)

        contentItems.forEach { item ->
            builder.beginControlFlow("composable(%S)", item.id)
            val funcName = item.functionName
            val pkg = item.packageName
            builder.addStatement("%M()", MemberName(pkg, funcName))
            builder.endControlFlow()
        }

        builder.endControlFlow()
        builder.endControlFlow()
        builder.endControlFlow()
        builder.endControlFlow()

        return builder.build()
    }

    private fun generateItem(builder: CodeBlock.Builder, item: ItemData, childrenMap: Map<String, List<ItemData>>) {
         when (item) {
            is RailItemData -> {
                builder.add("azRailItem(\n")
                builder.indent()
                builder.addStatement("id = %S,", item.id)
                if (item.icon != 0) builder.addStatement("content = %L,", item.icon)
                if (item.text.isNotEmpty()) builder.addStatement("text = %S,", item.text)
                if (item.hasContent) {
                    builder.addStatement("route = %S,", item.id)
                }
                builder.unindent()
                builder.addStatement(")")
            }
            is NestedRailData -> {
                // NestedRail trigger
                builder.add("azNestedRail(\n")
                builder.indent()
                builder.addStatement("id = %S,", item.id)
                if (item.text.isNotEmpty()) builder.addStatement("text = %S,", item.text)
                // alignment? defaulting to VERTICAL
                 builder.addStatement("alignment = %T.VERTICAL,", ClassName("com.hereliesaz.aznavrail.model", "AzNestedRailAlignment"))
                builder.unindent()
                builder.beginControlFlow(")")

                // Children
                childrenMap[item.id]?.forEach { child ->
                     // Children of nested rail are typically rail items
                     // But if the child is NestedRailData (from annotation), we treat it as item
                     generateItem(builder, child, childrenMap)
                }

                builder.endControlFlow()
            }
            is RailHostData -> {
                 builder.add("azRailHostItem(\n")
                builder.indent()
                builder.addStatement("id = %S,", item.id)
                if (item.text.isNotEmpty()) builder.addStatement("text = %S,", item.text)
                builder.unindent()
                builder.addStatement(")")

                // Look for children defined via some other mechanism?
                // Currently our logic only supports children if they are NestedRailData with parent set.
                // If the user uses NestedRailData(parent="host_id"), we can treat it as child.
                // But we need to handle that in the grouping logic.
                // Since we used generic ItemData in map, we can support it.
                 childrenMap[item.id]?.forEach { child ->
                     // If parent is Host, use azRailSubItem
                     if (child is NestedRailData) { // reusing NestedRailData for subitem...
                          builder.add("azRailSubItem(\n")
                          builder.indent()
                          builder.addStatement("id = %S,", child.id)
                          builder.addStatement("hostId = %S,", item.id)
                          if (child.text.isNotEmpty()) builder.addStatement("text = %S,", child.text)
                          if (child.hasContent) builder.addStatement("route = %S,", child.id)
                          builder.unindent()
                          builder.addStatement(")")
                     }
                }
            }
        }
    }

    private data class AppConfig(val dock: String?, val orientation: String?)
    private interface ItemData { val id: String; val hasContent: Boolean; val functionName: String; val packageName: String; val symbol: KSNode }
    private data class RailItemData(override val id: String, val text: String, val icon: Int, val isHost: Boolean, override val hasContent: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData
    private data class NestedRailData(override val id: String, val parent: String, val text: String, val icon: Int, override val hasContent: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData
    private data class RailHostData(override val id: String, val text: String, val icon: Int, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData { override val hasContent = false }

    private fun extractAppConfig(activity: KSClassDeclaration): AppConfig {
        val azAnnot = activity.getAnnotation("com.hereliesaz.aznavrail.annotation.Az") ?: return AppConfig(null, null)
        val appAnnot = azAnnot.getArgument("app") as? KSAnnotation ?: return AppConfig(null, null)

        val dockArg = appAnnot.arguments.find { it.name?.asString() == "dock" }?.value
        val dock = (dockArg as? KSType)?.declaration?.simpleName?.asString() ?: dockArg?.toString()?.substringAfterLast(".")

        val orientationArg = appAnnot.arguments.find { it.name?.asString() == "orientation" }?.value
        val orientation = (orientationArg as? KSType)?.declaration?.simpleName?.asString() ?: orientationArg?.toString()?.substringAfterLast(".")

        return AppConfig(dock, orientation)
    }

    private fun extractItems(symbols: List<KSAnnotated>, activityClass: KSClassDeclaration): List<ItemData> {
        val items = mutableListOf<ItemData>()

        symbols.forEach { symbol ->
            if (symbol is KSFunctionDeclaration || symbol is KSPropertyDeclaration) {
                val azAnnot = symbol.getAnnotation("com.hereliesaz.aznavrail.annotation.Az") ?: return@forEach

                // Composable Validation
                if (symbol is KSFunctionDeclaration) {
                    val isComposable = symbol.annotations.any {
                        it.shortName.asString() == "Composable" ||
                        it.annotationType.resolve().declaration.qualifiedName?.asString() == "androidx.compose.runtime.Composable"
                    }
                    if (!isComposable) {
                        logger.error("Function '${symbol.simpleName.asString()}' annotated with @Az must be @Composable.", symbol)
                        return@forEach // Skip this item
                    }
                }

                val railAnnot = azAnnot.getArgument("rail") as? KSAnnotation
                val hostAnnot = azAnnot.getArgument("host") as? KSAnnotation
                val nestedAnnot = azAnnot.getArgument("nested") as? KSAnnotation

                val name = symbol.simpleName.asString()
                val pkg = symbol.packageName.asString()
                val hasContent = symbol is KSFunctionDeclaration // Already validated if function

                val inferredId = name.toSnakeCase()
                val inferredText = name.splitCamelCase()

                val isRail = railAnnot?.getArgument("isValid") as? Boolean ?: false
                val isHost = hostAnnot?.getArgument("isValid") as? Boolean ?: false
                val isNested = nestedAnnot?.getArgument("isValid") as? Boolean ?: false

                if (isRail) {
                    items.add(RailItemData(
                        id = (railAnnot!!.getArgument("id") as? String)?.takeIf { it.isNotEmpty() } ?: inferredId,
                        text = (railAnnot.getArgument("text") as? String)?.takeIf { it.isNotEmpty() } ?: inferredText,
                        icon = (railAnnot.getArgument("icon") as? Int) ?: 0,
                        isHost = (railAnnot.getArgument("isHost") as? Boolean) ?: false,
                        hasContent = hasContent,
                        functionName = name,
                        packageName = pkg,
                        symbol = symbol
                    ))
                } else if (isHost) {
                     items.add(RailHostData(
                        id = (hostAnnot!!.getArgument("id") as? String)?.takeIf { it.isNotEmpty() } ?: inferredId,
                        text = (hostAnnot.getArgument("text") as? String)?.takeIf { it.isNotEmpty() } ?: inferredText,
                        icon = (hostAnnot.getArgument("icon") as? Int) ?: 0,
                        functionName = name,
                        packageName = pkg,
                        symbol = symbol
                    ))
                } else if (isNested) {
                    items.add(NestedRailData(
                        id = (nestedAnnot!!.getArgument("id") as? String)?.takeIf { it.isNotEmpty() } ?: inferredId,
                        parent = (nestedAnnot.getArgument("parent") as? String) ?: "",
                        text = (nestedAnnot.getArgument("text") as? String)?.takeIf { it.isNotEmpty() } ?: inferredText,
                        icon = (nestedAnnot.getArgument("icon") as? Int) ?: 0,
                        hasContent = hasContent,
                        functionName = name,
                        packageName = pkg,
                        symbol = symbol
                    ))
                } else {
                    if (symbol is KSFunctionDeclaration) {
                        items.add(RailItemData(
                            id = inferredId,
                            text = inferredText,
                            icon = 0,
                            isHost = false,
                            hasContent = hasContent,
                            functionName = name,
                            packageName = pkg,
                            symbol = symbol
                        ))
                    } else if (symbol is KSPropertyDeclaration) {
                         items.add(RailHostData(
                            id = inferredId,
                            text = inferredText,
                            icon = 0,
                            functionName = name,
                            packageName = pkg,
                            symbol = symbol
                        ))
                    }
                }
            }
        }
        return items
    }

    private fun KSAnnotated.getAnnotation(qName: String): KSAnnotation? {
        return annotations.find { it.annotationType.resolve().declaration.qualifiedName?.asString() == qName }
    }

    private fun KSAnnotation.getArgument(name: String): Any? {
        return arguments.find { it.name?.asString() == name }?.value
    }

    private fun String.toSnakeCase(): String {
        return this.replace(Regex("([a-z])([A-Z]+)"), "$1_$2").lowercase()
    }

    private fun String.splitCamelCase(): String {
        return this.replace(Regex("([a-z])([A-Z]+)"), "$1 $2")
    }
}
