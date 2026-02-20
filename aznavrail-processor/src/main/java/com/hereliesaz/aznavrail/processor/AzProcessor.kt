// aznavrail-processor/src/main/java/com/hereliesaz/aznavrail/processor/AzProcessor.kt
package com.hereliesaz.aznavrail.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo

class AzProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private var isGenerated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (isGenerated) return emptyList()

        val symbols = resolver.getSymbolsWithAnnotation("com.hereliesaz.aznavrail.annotation.Az").toList()
        if (symbols.isEmpty()) return emptyList()

        val activityClass = symbols.filterIsInstance<KSClassDeclaration>().firstOrNull() ?: return emptyList()
        val packageName = activityClass.packageName.asString()
        val graphClassName = "AzGraph"
        val mainActivityName = activityClass.qualifiedName?.asString() ?: return emptyList()

        val fileSpec = FileSpec.builder(packageName, graphClassName)
            .addImport("androidx.compose.runtime", "Composable")
            .addImport("androidx.activity.compose", "setContent")
            .addImport("androidx.navigation.compose", "rememberNavController", "composable")
            .addImport("com.hereliesaz.aznavrail", "AzHostActivityLayout", "AzNavHost", "AzGraphInterface", "AzActivity")
            .addImport("com.hereliesaz.aznavrail.model", "AzDockingSide", "AzNestedRailAlignment")
            .addImport("androidx.activity", "ComponentActivity")
            .addImport("androidx.compose.ui.unit", "dp")

        val graphObject = TypeSpec.objectBuilder(graphClassName)
            .addSuperinterface(ClassName("com.hereliesaz.aznavrail", "AzGraphInterface"))

        val runFunction = FunSpec.builder("Run")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("activity", ClassName("androidx.activity", "ComponentActivity"))
            .addCode(generateRunBody(activityClass, mainActivityName, symbols))

        graphObject.addFunction(runFunction.build())
        fileSpec.addType(graphObject.build())
        fileSpec.build().writeTo(codeGenerator, Dependencies(true, *symbols.mapNotNull { (it as? KSDeclaration)?.containingFile }.toTypedArray()))
        isGenerated = true
        return emptyList()
    }

    private fun generateRunBody(activityClass: KSClassDeclaration, mainActivityName: String, symbols: List<KSAnnotated>): CodeBlock {
        val appConfig = extractAppConfig(activityClass)
        val themeConfig = extractThemeConfig(activityClass)
        val advancedConfig = extractAdvancedConfig(activityClass)
        val items = extractItems(symbols, activityClass)

        val builder = CodeBlock.builder()
        
        // 1. Establish the Instance Context
        // This is the cure for Scope Amnesia. We cast the activity so we can access internal states.
        builder.addStatement("val instance = activity as? %T", ClassName.bestGuess(mainActivityName))
        
        builder.beginControlFlow("activity.setContent")
        builder.addStatement("val navController = rememberNavController()")

        val backgroundItems = items.filterIsInstance<BackgroundData>()
        val contentItems = items.filter { it.hasContent && it !is BackgroundData }
        val homeItem = items.filterIsInstance<RailItemData>().find { it.isHome && it.hasContent }
        val startDest = homeItem?.id ?: contentItems.firstOrNull()?.id ?: "home"

        builder.add("AzHostActivityLayout(\n")
        builder.indent()
        builder.addStatement("navController = navController,")
        if (appConfig?.initiallyExpanded == true) builder.addStatement("initiallyExpanded = true,")
        if (appConfig?.disableSwipeToOpen == true) builder.addStatement("disableSwipeToOpen = true,")
        builder.unindent()
        builder.add(") {\n")
        builder.indent()

        backgroundItems.forEach { bg ->
            builder.beginControlFlow("background(weight = %L)", bg.weight)
            if (bg.isMember) {
                builder.addStatement("instance?.%L()", bg.functionName)
            } else {
                builder.addStatement("%M()", MemberName(bg.packageName, bg.functionName))
            }
            builder.endControlFlow()
        }

        builder.add("onscreen {\n")
        builder.indent()
        builder.beginControlFlow("AzNavHost(startDestination = %S)", startDest)
        contentItems.forEach { item ->
            builder.beginControlFlow("composable(%S)", item.id)
            if (item.isMember) {
                builder.addStatement("instance?.%L()", item.functionName)
            } else {
                builder.addStatement("%M()", MemberName(item.packageName, item.functionName))
            }
            builder.endControlFlow()
        }
        builder.endControlFlow()
        builder.unindent()
        builder.add("}\n")

        if (appConfig != null) {
            builder.add("azConfig(\n")
            builder.indent()
            builder.addStatement("dockingSide = %T.%L,", ClassName("com.hereliesaz.aznavrail.model", "AzDockingSide"), appConfig.dock)
            builder.addStatement("packButtons = %L,", appConfig.packButtons)
            builder.addStatement("noMenu = %L,", appConfig.noMenu)
            builder.addStatement("vibrate = %L,", appConfig.vibrate)
            builder.addStatement("displayAppName = %L,", appConfig.displayAppName)
            builder.addStatement("usePhysicalDocking = %L,", appConfig.usePhysicalDocking)
            builder.addStatement("showFooter = %L,", appConfig.showFooter)
            if (appConfig.expandedWidth > 0) builder.addStatement("expandedWidth = %L.dp,", appConfig.expandedWidth)
            if (appConfig.collapsedWidth > 0) builder.addStatement("collapsedWidth = %L.dp,", appConfig.collapsedWidth)
            if (appConfig.activeClassifiers.isNotEmpty()) {
                builder.addStatement("activeClassifiers = setOf(%L),", appConfig.activeClassifiers.joinToString(", ") { "\"$it\"" })
            }
            builder.unindent()
            builder.addStatement(")\n")
        }

        if (themeConfig != null) {
            builder.add("azTheme(\n")
            builder.indent()
            if (themeConfig.activeColorHex.isNotEmpty()) {
                builder.addStatement("activeColor = %T(android.graphics.Color.parseColor(%S)),", ClassName("androidx.compose.ui.graphics", "Color"), themeConfig.activeColorHex)
            }
            builder.addStatement("defaultShape = %T.%L,", ClassName("com.hereliesaz.aznavrail.model", "AzButtonShape"), themeConfig.defaultShape)
            builder.addStatement("headerIconShape = %T.%L,", ClassName("com.hereliesaz.aznavrail.model", "AzHeaderIconShape"), themeConfig.headerIconShape)
            builder.unindent()
            builder.addStatement(")\n")
        }

        if (advancedConfig != null) {
            builder.add("azAdvanced(\n")
            builder.indent()
            if (advancedConfig.isLoading) builder.addStatement("isLoading = instance?.%L ?: false,", advancedConfig.functionName)
            if (advancedConfig.infoScreen) {
                builder.addStatement("infoScreen = instance?.%L ?: false,", advancedConfig.functionName)
                builder.addStatement("onDismissInfoScreen = { instance?.%L = false },", advancedConfig.functionName)
            }
            if (advancedConfig.enableRailDragging) builder.addStatement("enableRailDragging = true,")
            if (advancedConfig.overlayServiceClass.isNotEmpty()) builder.addStatement("overlayService = %T::class.java,", ClassName.bestGuess(advancedConfig.overlayServiceClass))
            
            // Fix for Reflection Trap: Bind directly to the instance
            if (advancedConfig.onUndock.isNotEmpty()) builder.addStatement("onUndock = { instance?.%L() },", advancedConfig.onUndock)
            if (advancedConfig.onRailDrag.isNotEmpty()) builder.addStatement("onRailDrag = { x, y -> instance?.%L(x, y) },", advancedConfig.onRailDrag)
            if (advancedConfig.onOverlayDrag.isNotEmpty()) builder.addStatement("onOverlayDrag = { x, y -> instance?.%L(x, y) },", advancedConfig.onOverlayDrag)
            if (advancedConfig.onItemGloballyPositioned.isNotEmpty()) builder.addStatement("onItemGloballyPositioned = { id, rect -> instance?.%L(id, rect) },", advancedConfig.onItemGloballyPositioned)
            builder.unindent()
            builder.addStatement(")\n")
        }

        builder.addStatement("val azActivity = activity as? %T", ClassName("com.hereliesaz.aznavrail", "AzActivity"))
        builder.addStatement("azActivity?.apply { configureRail() }")
        builder.addStatement("")

        val topLevelItems = mutableListOf<ItemData>()
        val childrenMap = mutableMapOf<String, MutableList<ItemData>>()
        items.filter { it !is BackgroundData && it !is AdvancedData }.forEach { item ->
            if (item.parent.isNotEmpty()) childrenMap.getOrPut(item.parent) { mutableListOf() }.add(item) else topLevelItems.add(item)
        }

        topLevelItems.forEach { generateItem(builder, it, childrenMap) }

        builder.unindent()
        builder.addStatement("}")
        builder.endControlFlow()
        return builder.build()
    }

    private fun generateItem(builder: CodeBlock.Builder, item: ItemData, childrenMap: Map<String, List<ItemData>>) {
         when (item) {
            is RailItemData -> {
                val prefix = if (item.isMenu) "azMenuItem" else "azRailItem"
                builder.add("$prefix(\n")
                builder.indent()
                builder.addStatement("id = %S,", item.id)
                
                if (!item.isMenu) {
                    if (item.iconText.isNotEmpty()) {
                        builder.addStatement("content = %S,", item.iconText)
                    } else if (item.icon != 0) {
                        builder.addStatement("content = %L,", item.icon)
                    }
                }
                
                if (item.text.isNotEmpty()) builder.addStatement("text = %S,", item.text)
                addMetadataParameters(builder, item)
                
                if (!item.isMenu) {
                    if (item.classifiers.isNotEmpty()) builder.addStatement("classifiers = setOf(%L),", item.classifiers.joinToString(", ") { "\"$it\"" })
                    if (item.onFocus.isNotEmpty()) builder.addStatement("onFocus = { instance?.%L() },", item.onFocus)
                }
                
                if (item.hasContent) builder.addStatement("route = %S,", item.id)
                else if (item.isAction) {
                    if (item.isMember) builder.addStatement("onClick = { instance?.%L() },", item.functionName)
                    else builder.addStatement("onClick = { %M() },", MemberName(item.packageName, item.functionName))
                }
                else builder.addStatement("onClick = {},")
                
                builder.unindent()
                builder.addStatement(")")
            }
            is NestedRailData -> {
                builder.add("azNestedRail(\n")
                builder.indent()
                builder.addStatement("id = %S,", item.id)
                if (item.text.isNotEmpty()) builder.addStatement("text = %S,", item.text)
                addMetadataParameters(builder, item)
                if (item.classifiers.isNotEmpty()) builder.addStatement("classifiers = setOf(%L),", item.classifiers.joinToString(", ") { "\"$it\"" })
                if (item.onFocus.isNotEmpty()) builder.addStatement("onFocus = { instance?.%L() },", item.onFocus)
                
                builder.addStatement("alignment = %T.%L,", ClassName("com.hereliesaz.aznavrail.model", "AzNestedRailAlignment"), item.alignment)
                builder.unindent()
                builder.beginControlFlow(")")
                childrenMap[item.id]?.forEach { generateItem(builder, it, childrenMap) }
                builder.endControlFlow()
            }
            is RailHostData -> {
                val prefix = if (item.isMenu) "azMenuHostItem" else "azRailHostItem"
                builder.add("$prefix(\n")
                builder.indent()
                builder.addStatement("id = %S,", item.id)
                if (item.text.isNotEmpty()) builder.addStatement("text = %S,", item.text)
                addMetadataParameters(builder, item)
                builder.addStatement("onClick = {},")
                builder.unindent()
                builder.addStatement(")")

                 childrenMap[item.id]?.forEach { child ->
                     if (child is RailItemData) {
                          val subPrefix = if (item.isMenu) "azMenuSubItem" else "azRailSubItem"
                          builder.add("$subPrefix(\n")
                          builder.indent()
                          builder.addStatement("id = %S,", child.id)
                          builder.addStatement("hostId = %S,", item.id)
                          if (child.text.isNotEmpty()) builder.addStatement("text = %S,", child.text)
                          addMetadataParameters(builder, child)
                          if (!item.isMenu) {
                              if (child.classifiers.isNotEmpty()) builder.addStatement("classifiers = setOf(%L),", child.classifiers.joinToString(", ") { "\"$it\"" })
                              if (child.onFocus.isNotEmpty()) builder.addStatement("onFocus = { instance?.%L() },", child.onFocus)
                          }
                          if (child.hasContent) builder.addStatement("route = %S,", child.id)
                          else if (child.isAction) {
                              if (child.isMember) builder.addStatement("onClick = { instance?.%L() },", child.functionName)
                              else builder.addStatement("onClick = { %M() },", MemberName(child.packageName, child.functionName))
                          }
                          else builder.addStatement("onClick = {},")
                          builder.unindent()
                          builder.addStatement(")")
                     } else if (child is ToggleData || child is CyclerData || child is RelocItemData) {
                          generateItem(builder, child, childrenMap)
                     }
                }
            }
            is ToggleData -> {
                val prefix = when { item.parent.isNotEmpty() && item.isMenu -> "azMenuSubToggle"; item.parent.isNotEmpty() && !item.isMenu -> "azRailSubToggle"; item.isMenu -> "azMenuToggle"; else -> "azRailToggle" }
                builder.add("$prefix(\n")
                builder.indent()
                builder.addStatement("id = %S,", item.id)
                if (item.parent.isNotEmpty()) builder.addStatement("hostId = %S,", item.parent)
                builder.addStatement("toggleOnText = %S,", item.toggleOnText)
                builder.addStatement("toggleOffText = %S,", item.toggleOffText)
                addMetadataParameters(builder, item)

                if (item.isProperty) {
                    if (item.isMember) {
                        builder.addStatement("isChecked = instance?.%L ?: false,", item.functionName)
                        builder.addStatement("onClick = { instance?.let { it.%L = !it.%L } },", item.functionName, item.functionName)
                    } else {
                        val prop = MemberName(item.packageName, item.functionName)
                        builder.addStatement("isChecked = %M,", prop)
                        builder.addStatement("onClick = { %M = !%M },", prop, prop)
                    }
                } else if (item.isAction) {
                    builder.addStatement("isChecked = false,")
                    if (item.isMember) builder.addStatement("onClick = { instance?.%L() },", item.functionName)
                    else builder.addStatement("onClick = { %M() },", MemberName(item.packageName, item.functionName))
                } else {
                    builder.addStatement("isChecked = false,")
                    builder.addStatement("onClick = {},")
                }
                builder.unindent()
                builder.addStatement(")")
            }
            is CyclerData -> {
                val prefix = when { item.parent.isNotEmpty() && item.isMenu -> "azMenuSubCycler"; item.parent.isNotEmpty() && !item.isMenu -> "azRailSubCycler"; item.isMenu -> "azMenuCycler"; else -> "azRailCycler" }
                builder.add("$prefix(\n")
                builder.indent()
                builder.addStatement("id = %S,", item.id)
                if (item.parent.isNotEmpty()) builder.addStatement("hostId = %S,", item.parent)
                val optionsStr = item.options.joinToString(", ") { "\"$it\"" }
                builder.addStatement("options = listOf(%L),", optionsStr)
                addMetadataParameters(builder, item)
                if (item.disabledOptions.isNotEmpty()) builder.addStatement("disabledOptions = listOf(%L),", item.disabledOptions.joinToString(", ") { "\"$it\"" })
                
                if (item.isProperty) {
                    if (item.isMember) {
                        builder.addStatement("selectedOption = instance?.%L ?: %S,", item.functionName, item.options.firstOrNull() ?: "")
                        builder.add("onClick = {\n")
                        builder.indent()
                        builder.addStatement("val opts = listOf(%L)", optionsStr)
                        builder.beginControlFlow("if (opts.isNotEmpty() && instance != null)")
                        builder.addStatement("val idx = opts.indexOf(instance.%L)", item.functionName)
                        builder.addStatement("instance.%L = opts[(idx + 1) %% opts.size]", item.functionName)
                        builder.endControlFlow()
                        builder.unindent()
                        builder.addStatement("},\n")
                    } else {
                        val prop = MemberName(item.packageName, item.functionName)
                        builder.addStatement("selectedOption = %M,", prop)
                        builder.add("onClick = {\n")
                        builder.indent()
                        builder.addStatement("val opts = listOf(%L)", optionsStr)
                        builder.beginControlFlow("if (opts.isNotEmpty())")
                        builder.addStatement("val idx = opts.indexOf(%M)", prop)
                        builder.addStatement("%M = opts[(idx + 1) %% opts.size]", prop)
                        builder.endControlFlow()
                        builder.unindent()
                        builder.addStatement("},\n")
                    }
                } else if (item.isAction) {
                    builder.addStatement("selectedOption = %S,", item.options.firstOrNull() ?: "")
                    if (item.isMember) builder.addStatement("onClick = { instance?.%L() },", item.functionName)
                    else builder.addStatement("onClick = { %M() },", MemberName(item.packageName, item.functionName))
                } else {
                    builder.addStatement("selectedOption = %S,", item.options.firstOrNull() ?: "")
                    builder.addStatement("onClick = {},")
                }
                builder.unindent()
                builder.addStatement(")")
            }
            is RelocItemData -> {
                builder.add("azRailRelocItem(\n")
                builder.indent()
                builder.addStatement("id = %S,", item.id)
                builder.addStatement("hostId = %S,", item.parent)
                if (item.text.isNotEmpty()) builder.addStatement("text = %S,", item.text)
                addMetadataParameters(builder, item)
                if (item.classifiers.isNotEmpty()) builder.addStatement("classifiers = setOf(%L),", item.classifiers.joinToString(", ") { "\"$it\"" })
                if (item.onFocus.isNotEmpty()) builder.addStatement("onFocus = { instance?.%L() },", item.onFocus)
                if (item.onRelocate.isNotEmpty()) builder.addStatement("onRelocate = { a, b, c -> instance?.%L(a, b, c) },", item.onRelocate)
                
                if (item.hasContent) builder.addStatement("route = %S,", item.id)
                else if (item.isAction) {
                    if (item.isMember) builder.addStatement("onClick = { instance?.%L() },", item.functionName)
                    else builder.addStatement("onClick = { %M() },", MemberName(item.packageName, item.functionName))
                }
                else builder.addStatement("onClick = {},")
                
                builder.unindent()
                builder.beginControlFlow(") {")
                item.hiddenMenuRoutes.forEach { route -> builder.addStatement("listItem(%S, %S)", route.splitCamelCase(), route) }
                item.hiddenMenuActions.forEach { action -> builder.addStatement("listItem(%S) { instance?.%L() }", action.splitCamelCase(), action) }
                item.hiddenMenuInputs.forEach { input -> builder.addStatement("inputItem(%S) { value -> instance?.%L(value) }", input.splitCamelCase(), input) }
                builder.endControlFlow()
            }
            is DividerData -> builder.addStatement("azDivider()")
            else -> {}
        }
    }
    
    private fun addMetadataParameters(builder: CodeBlock.Builder, item: MetadataItem) {
        if (item.disabled) builder.addStatement("disabled = true,")
        if (item.screenTitle.isNotEmpty()) builder.addStatement("screenTitle = %S,", item.screenTitle)
        if (item.info.isNotEmpty()) builder.addStatement("info = %S,", item.info)
    }

    private data class AppConfig(val dock: String, val packButtons: Boolean, val noMenu: Boolean, val vibrate: Boolean, val displayAppName: Boolean, val usePhysicalDocking: Boolean, val showFooter: Boolean, val expandedWidth: Int, val collapsedWidth: Int, val initiallyExpanded: Boolean, val disableSwipeToOpen: Boolean, val activeClassifiers: List<String>)
    private data class ThemeConfig(val activeColorHex: String, val defaultShape: String, val headerIconShape: String)
    private interface ItemData { val id: String; val parent: String; val hasContent: Boolean; val isAction: Boolean; val isProperty: Boolean; val isMember: Boolean; val functionName: String; val packageName: String; val symbol: KSNode }
    private interface MetadataItem { val disabled: Boolean; val screenTitle: String; val info: String }
    private data class RailItemData(override val id: String, override val parent: String, val text: String, val icon: Int, val iconText: String, val isHost: Boolean, val isHome: Boolean, val isMenu: Boolean, override val disabled: Boolean, override val screenTitle: String, override val info: String, val classifiers: List<String>, val onFocus: String, override val hasContent: Boolean, override val isAction: Boolean, override val isProperty: Boolean, override val isMember: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData, MetadataItem
    private data class NestedRailData(override val id: String, override val parent: String, val text: String, val icon: Int, val iconText: String, override val disabled: Boolean, override val screenTitle: String, override val info: String, val classifiers: List<String>, val onFocus: String, val alignment: String, override val hasContent: Boolean, override val isAction: Boolean, override val isProperty: Boolean, override val isMember: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData, MetadataItem
    private data class RailHostData(override val id: String, val text: String, val icon: Int, val iconText: String, val isMenu: Boolean, override val disabled: Boolean, override val screenTitle: String, override val info: String, override val isMember: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData, MetadataItem { override val hasContent = false; override val isAction = false; override val isProperty = false; override val parent = "" }
    private data class BackgroundData(val weight: Int, override val isMember: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData { override val id = ""; override val parent = ""; override val hasContent = true; override val isAction = false; override val isProperty = false }
    private data class ToggleData(override val id: String, override val parent: String, val toggleOnText: String, val toggleOffText: String, val isMenu: Boolean, override val disabled: Boolean, override val screenTitle: String, override val info: String, override val hasContent: Boolean, override val isAction: Boolean, override val isProperty: Boolean, override val isMember: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData, MetadataItem
    private data class CyclerData(override val id: String, override val parent: String, val options: List<String>, val isMenu: Boolean, override val disabled: Boolean, val disabledOptions: List<String>, override val screenTitle: String, override val info: String, override val hasContent: Boolean, override val isAction: Boolean, override val isProperty: Boolean, override val isMember: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData, MetadataItem
    private data class RelocItemData(override val id: String, override val parent: String, val text: String, val hiddenMenuRoutes: List<String>, val hiddenMenuActions: List<String>, val hiddenMenuInputs: List<String>, override val disabled: Boolean, override val screenTitle: String, override val info: String, val classifiers: List<String>, val onFocus: String, val onRelocate: String, override val hasContent: Boolean, override val isAction: Boolean, override val isMember: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData, MetadataItem { override val isProperty = false }
    private data class DividerData(override val symbol: KSNode) : ItemData { override val id = ""; override val parent = ""; override val hasContent = false; override val isAction = false; override val isProperty = false; override val isMember = false; override val functionName = ""; override val packageName = "" }
    private data class AdvancedData(val isLoading: Boolean, val infoScreen: Boolean, val enableRailDragging: Boolean, val overlayServiceClass: String, val onUndock: String, val onRailDrag: String, val onOverlayDrag: String, val onItemGloballyPositioned: String, override val isMember: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData { override val id = ""; override val parent = ""; override val hasContent = false; override val isAction = false; override val isProperty = true }

    private fun extractAppConfig(activity: KSClassDeclaration): AppConfig? {
        val azAnnot = activity.getAnnotation("com.hereliesaz.aznavrail.annotation.Az") ?: return null
        val appAnnot = azAnnot.getArgument("app") as? KSAnnotation ?: return null
        val dockArg = appAnnot.arguments.find { it.name?.asString() == "dock" }?.value
        val dock = (dockArg as? KSType)?.declaration?.simpleName?.asString() ?: dockArg?.toString()?.substringAfterLast(".") ?: "LEFT"
        
        return AppConfig(
            dock = dock, packButtons = (appAnnot.getArgument("packButtons") as? Boolean) ?: false,
            noMenu = (appAnnot.getArgument("noMenu") as? Boolean) ?: false, vibrate = (appAnnot.getArgument("vibrate") as? Boolean) ?: false,
            displayAppName = (appAnnot.getArgument("displayAppName") as? Boolean) ?: false, usePhysicalDocking = (appAnnot.getArgument("usePhysicalDocking") as? Boolean) ?: false,
            showFooter = (appAnnot.getArgument("showFooter") as? Boolean) ?: true, expandedWidth = (appAnnot.getArgument("expandedWidth") as? Int) ?: -1,
            collapsedWidth = (appAnnot.getArgument("collapsedWidth") as? Int) ?: -1, initiallyExpanded = (appAnnot.getArgument("initiallyExpanded") as? Boolean) ?: false,
            disableSwipeToOpen = (appAnnot.getArgument("disableSwipeToOpen") as? Boolean) ?: false,
            activeClassifiers = (appAnnot.getArgument("activeClassifiers") as? ArrayList<*>)?.map { it.toString() } ?: emptyList()
        )
    }

    private fun extractThemeConfig(activity: KSClassDeclaration): ThemeConfig? {
        val azAnnot = activity.getAnnotation("com.hereliesaz.aznavrail.annotation.Az") ?: return null
        val themeAnnot = azAnnot.getArgument("theme") as? KSAnnotation ?: return null
        if (!(themeAnnot.getArgument("isValid") as? Boolean ?: false)) return null

        val shapeArg = themeAnnot.arguments.find { it.name?.asString() == "defaultShape" }?.value
        val defaultShape = (shapeArg as? KSType)?.declaration?.simpleName?.asString() ?: shapeArg?.toString()?.substringAfterLast(".") ?: "CIRCLE"
        val headerShapeArg = themeAnnot.arguments.find { it.name?.asString() == "headerIconShape" }?.value
        val headerIconShape = (headerShapeArg as? KSType)?.declaration?.simpleName?.asString() ?: headerShapeArg?.toString()?.substringAfterLast(".") ?: "CIRCLE"

        return ThemeConfig(activeColorHex = (themeAnnot.getArgument("activeColorHex") as? String) ?: "", defaultShape = defaultShape, headerIconShape = headerIconShape)
    }

    private fun extractAdvancedConfig(activity: KSClassDeclaration): AdvancedData? {
        val azAnnot = activity.getAnnotation("com.hereliesaz.aznavrail.annotation.Az") ?: return null
        val advAnnot = azAnnot.getArgument("advanced") as? KSAnnotation ?: return null
        if (!(advAnnot.getArgument("isValid") as? Boolean ?: false)) return null
        
        val isMember = activity.parentDeclaration is KSClassDeclaration
        
        return AdvancedData(
            isLoading = (advAnnot.getArgument("isLoading") as? Boolean) ?: false, infoScreen = (advAnnot.getArgument("infoScreen") as? Boolean) ?: false,
            enableRailDragging = (advAnnot.getArgument("enableRailDragging") as? Boolean) ?: false, overlayServiceClass = (advAnnot.getArgument("overlayServiceClass") as? String) ?: "",
            onUndock = (advAnnot.getArgument("onUndock") as? String) ?: "", onRailDrag = (advAnnot.getArgument("onRailDrag") as? String) ?: "",
            onOverlayDrag = (advAnnot.getArgument("onOverlayDrag") as? String) ?: "", onItemGloballyPositioned = (advAnnot.getArgument("onItemGloballyPositioned") as? String) ?: "",
            isMember = isMember, functionName = "", packageName = "", symbol = activity
        )
    }

    private fun extractItems(symbols: List<KSAnnotated>, activityClass: KSClassDeclaration): List<ItemData> {
        val items = mutableListOf<ItemData>()
        symbols.forEach { symbol ->
            if (symbol is KSFunctionDeclaration || symbol is KSPropertyDeclaration) {
                val azAnnot = symbol.getAnnotation("com.hereliesaz.aznavrail.annotation.Az") ?: return@forEach
                val railAnnot = azAnnot.getArgument("rail") as? KSAnnotation
                val menuAnnot = azAnnot.getArgument("menu") as? KSAnnotation
                val hostAnnot = azAnnot.getArgument("host") as? KSAnnotation
                val nestedAnnot = azAnnot.getArgument("nested") as? KSAnnotation
                val bgAnnot = azAnnot.getArgument("background") as? KSAnnotation
                val toggleAnnot = azAnnot.getArgument("toggle") as? KSAnnotation
                val cyclerAnnot = azAnnot.getArgument("cycler") as? KSAnnotation
                val relocAnnot = azAnnot.getArgument("reloc") as? KSAnnotation
                val dividerAnnot = azAnnot.getArgument("divider") as? KSAnnotation
                
                val name = symbol.simpleName.asString()
                val pkg = symbol.packageName.asString()
                
                val isComposable = symbol.annotations.any { it.shortName.asString() == "Composable" }
                val isFunction = symbol is KSFunctionDeclaration
                val isProperty = symbol is KSPropertyDeclaration
                val hasContent = isFunction && isComposable
                val isAction = isFunction && !isComposable
                val isMember = symbol.parentDeclaration is KSClassDeclaration
                
                val inferredId = name.toSnakeCase()
                val inferredText = name.splitCamelCase()

                if ((railAnnot?.getArgument("isValid") as? Boolean) == true || (menuAnnot?.getArgument("isValid") as? Boolean) == true) {
                    val activeAnnot = if ((railAnnot?.getArgument("isValid") as? Boolean) == true) railAnnot!! else menuAnnot!!
                    items.add(RailItemData(
                        id = (activeAnnot.getArgument("id") as? String)?.takeIf { it.isNotEmpty() } ?: inferredId, parent = (activeAnnot.getArgument("parent") as? String) ?: "",
                        text = (activeAnnot.getArgument("text") as? String)?.takeIf { it.isNotEmpty() } ?: inferredText, icon = (activeAnnot.getArgument("icon") as? Int) ?: 0,
                        iconText = (activeAnnot.getArgument("iconText") as? String) ?: "",
                        isHost = false, isHome = if (activeAnnot == railAnnot) (activeAnnot.getArgument("home") as? Boolean) ?: false else false,
                        isMenu = activeAnnot == menuAnnot, disabled = (activeAnnot.getArgument("disabled") as? Boolean) ?: false, screenTitle = (activeAnnot.getArgument("screenTitle") as? String) ?: "",
                        info = (activeAnnot.getArgument("info") as? String) ?: "", classifiers = (activeAnnot.getArgument("classifiers") as? ArrayList<*>)?.map { it.toString() } ?: emptyList(),
                        onFocus = if (activeAnnot == railAnnot) (activeAnnot.getArgument("onFocus") as? String) ?: "" else "", hasContent = hasContent, isAction = isAction, isProperty = isProperty, isMember = isMember, functionName = name, packageName = pkg, symbol = symbol
                    ))
                } else if ((hostAnnot?.getArgument("isValid") as? Boolean) == true) {
                     items.add(RailHostData(id = (hostAnnot!!.getArgument("id") as? String)?.takeIf { it.isNotEmpty() } ?: inferredId, text = (hostAnnot.getArgument("text") as? String)?.takeIf { it.isNotEmpty() } ?: inferredText, icon = (hostAnnot.getArgument("icon") as? Int) ?: 0, iconText = (hostAnnot.getArgument("iconText") as? String) ?: "", isMenu = (hostAnnot.getArgument("isMenu") as? Boolean) ?: false, disabled = (hostAnnot.getArgument("disabled") as? Boolean) ?: false, screenTitle = (hostAnnot.getArgument("screenTitle") as? String) ?: "", info = (hostAnnot.getArgument("info") as? String) ?: "", isMember = isMember, functionName = name, packageName = pkg, symbol = symbol))
                } else if ((nestedAnnot?.getArgument("isValid") as? Boolean) == true) {
                    val alignArg = nestedAnnot!!.arguments.find { it.name?.asString() == "alignment" }?.value
                    val alignment = (alignArg as? KSType)?.declaration?.simpleName?.asString() ?: alignArg?.toString()?.substringAfterLast(".") ?: "VERTICAL"
                    items.add(NestedRailData(id = (nestedAnnot.getArgument("id") as? String)?.takeIf { it.isNotEmpty() } ?: inferredId, parent = (nestedAnnot.getArgument("parent") as? String) ?: "", text = (nestedAnnot.getArgument("text") as? String)?.takeIf { it.isNotEmpty() } ?: inferredText, icon = (nestedAnnot.getArgument("icon") as? Int) ?: 0, iconText = (nestedAnnot.getArgument("iconText") as? String) ?: "", disabled = (nestedAnnot.getArgument("disabled") as? Boolean) ?: false, screenTitle = (nestedAnnot.getArgument("screenTitle") as? String) ?: "", info = (nestedAnnot.getArgument("info") as? String) ?: "", classifiers = (nestedAnnot.getArgument("classifiers") as? ArrayList<*>)?.map { it.toString() } ?: emptyList(), onFocus = (nestedAnnot.getArgument("onFocus") as? String) ?: "", alignment = alignment, hasContent = hasContent, isAction = isAction, isProperty = isProperty, isMember = isMember, functionName = name, packageName = pkg, symbol = symbol))
                } else if ((bgAnnot?.getArgument("isValid") as? Boolean) == true) {
                    items.add(BackgroundData(weight = (bgAnnot!!.getArgument("weight") as? Int) ?: 0, isMember = isMember, functionName = name, packageName = pkg, symbol = symbol))
                } else if ((toggleAnnot?.getArgument("isValid") as? Boolean) == true) {
                    items.add(ToggleData(id = (toggleAnnot!!.getArgument("id") as? String)?.takeIf { it.isNotEmpty() } ?: inferredId, parent = (toggleAnnot.getArgument("parent") as? String) ?: "", toggleOnText = (toggleAnnot.getArgument("toggleOnText") as? String) ?: "On", toggleOffText = (toggleAnnot.getArgument("toggleOffText") as? String) ?: "Off", isMenu = (toggleAnnot.getArgument("isMenu") as? Boolean) ?: false, disabled = (toggleAnnot.getArgument("disabled") as? Boolean) ?: false, screenTitle = (toggleAnnot.getArgument("screenTitle") as? String) ?: "", info = (toggleAnnot.getArgument("info") as? String) ?: "", hasContent = hasContent, isAction = isAction, isProperty = isProperty, isMember = isMember, functionName = name, packageName = pkg, symbol = symbol))
                } else if ((cyclerAnnot?.getArgument("isValid") as? Boolean) == true) {
                    items.add(CyclerData(id = (cyclerAnnot!!.getArgument("id") as? String)?.takeIf { it.isNotEmpty() } ?: inferredId, parent = (cyclerAnnot.getArgument("parent") as? String) ?: "", options = (cyclerAnnot.getArgument("options") as? ArrayList<*>)?.map { it.toString() } ?: emptyList(), isMenu = (cyclerAnnot.getArgument("isMenu") as? Boolean) ?: false, disabled = (cyclerAnnot.getArgument("disabled") as? Boolean) ?: false, disabledOptions = (cyclerAnnot.getArgument("disabledOptions") as? ArrayList<*>)?.map { it.toString() } ?: emptyList(), screenTitle = (cyclerAnnot.getArgument("screenTitle") as? String) ?: "", info = (cyclerAnnot.getArgument("info") as? String) ?: "", hasContent = hasContent, isAction = isAction, isProperty = isProperty, isMember = isMember, functionName = name, packageName = pkg, symbol = symbol))
                } else if ((relocAnnot?.getArgument("isValid") as? Boolean) == true) {
                    items.add(RelocItemData(id = (relocAnnot!!.getArgument("id") as? String)?.takeIf { it.isNotEmpty() } ?: inferredId, parent = (relocAnnot.getArgument("parent") as? String) ?: "", text = (relocAnnot.getArgument("text") as? String)?.takeIf { it.isNotEmpty() } ?: inferredText, hiddenMenuRoutes = (relocAnnot.getArgument("hiddenMenuRoutes") as? ArrayList<*>)?.map { it.toString() } ?: emptyList(), hiddenMenuActions = (relocAnnot.getArgument("hiddenMenuActions") as? ArrayList<*>)?.map { it.toString() } ?: emptyList(), hiddenMenuInputs = (relocAnnot.getArgument("hiddenMenuInputs") as? ArrayList<*>)?.map { it.toString() } ?: emptyList(), disabled = (relocAnnot.getArgument("disabled") as? Boolean) ?: false, screenTitle = (relocAnnot.getArgument("screenTitle") as? String) ?: "", info = (relocAnnot.getArgument("info") as? String) ?: "", classifiers = (relocAnnot.getArgument("classifiers") as? ArrayList<*>)?.map { it.toString() } ?: emptyList(), onFocus = (relocAnnot.getArgument("onFocus") as? String) ?: "", onRelocate = (relocAnnot.getArgument("onRelocate") as? String) ?: "", hasContent = hasContent, isAction = isAction, isMember = isMember, functionName = name, packageName = pkg, symbol = symbol))
                } else if ((dividerAnnot?.getArgument("isValid") as? Boolean) == true) {
                    items.add(DividerData(symbol))
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
