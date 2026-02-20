package com.hereliesaz.aznavrail.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
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
            if (bg.isMember) builder.addStatement("instance?.%L()", bg.functionName)
            else builder.addStatement("%M()", MemberName(bg.packageName, bg.functionName))
            builder.endControlFlow()
        }

        builder.add("onscreen {\n")
        builder.indent()
        builder.beginControlFlow("AzNavHost(startDestination = %S)", startDest)
        contentItems.forEach { item ->
            builder.beginControlFlow("composable(%S)", item.id)
            if (item.isMember) builder.addStatement("instance?.%L()", item.functionName)
            else builder.addStatement("%M()", MemberName(item.packageName, item.functionName))
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
        items.filter { it !is BackgroundData }.forEach { item ->
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
                injectIconContent(builder, item)
                if (item.text.isNotEmpty()) builder.addStatement("text = %S,", item.text)
                addMetadataParameters(builder, item)
                if (!item.isMenu) {
                    if (item.classifiers.isNotEmpty()) builder.addStatement("classifiers = setOf(%L),", item.classifiers.joinToString(", ") { "\"$it\"" })
                    if (item.onFocus.isNotEmpty()) builder.addStatement("onFocus = { instance?.%L() },", item.onFocus)
                }
                injectRouting(builder, item)
                builder.unindent()
                builder.addStatement(")")
            }
            is NestedRailData -> {
                builder.add("azNestedRail(\n")
                builder.indent()
                builder.addStatement("id = %S,", item.id)
                if (item.text.isNotEmpty()) builder.addStatement("text = %S,", item.text)
                injectIconContent(builder, item)
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
                injectIconContent(builder, item)
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
                          injectIconContent(builder, child)
                          addMetadataParameters(builder, child)
                          if (!item.isMenu) {
                              if (child.classifiers.isNotEmpty()) builder.addStatement("classifiers = setOf(%L),", child.classifiers.joinToString(", ") { "\"$it\"" })
                              if (child.onFocus.isNotEmpty()) builder.addStatement("onFocus = { instance?.%L() },", child.onFocus)
                          }
                          injectRouting(builder, child)
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
                injectIconContent(builder, item)
                addMetadataParameters(builder, item)
                if (item.classifiers.isNotEmpty()) builder.addStatement("classifiers = setOf(%L),", item.classifiers.joinToString(", ") { "\"$it\"" })
                if (item.onFocus.isNotEmpty()) builder.addStatement("onFocus = { instance?.%L() },", item.onFocus)
                if (item.onRelocate.isNotEmpty()) builder.addStatement("onRelocate = { a, b, c -> instance?.%L(a, b, c) },", item.onRelocate)
                injectRouting(builder, item)
                
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
    
    private fun injectIconContent(builder: CodeBlock.Builder, item: IconData) {
        if (item.iconText.isNotEmpty()) {
            builder.addStatement("content = %S,", item.iconText)
        } else if (item.icon != 0) {
            builder.addStatement("content = %L,", item.icon)
        }
    }

    private fun injectRouting(builder: CodeBlock.Builder, item: ItemData) {
        if (item.hasContent) builder.addStatement("route = %S,", item.id)
        else if (item.isAction) {
            if (item.isMember) builder.addStatement("onClick = { instance?.%L() },", item.functionName)
            else builder.addStatement("onClick = { %M() },", MemberName(item.packageName, item.functionName))
        }
        else builder.addStatement("onClick = {},")
    }

    private fun addMetadataParameters(builder: CodeBlock.Builder, item: MetadataItem) {
        if (item.disabled) builder.addStatement("disabled = true,")
        if (item.screenTitle.isNotEmpty()) builder.addStatement("screenTitle = %S,", item.screenTitle)
        if (item.info.isNotEmpty()) builder.addStatement("info = %S,", item.info)
    }

    private fun extractAppConfig(activityClass: KSClassDeclaration): AppConfig? {
        val azAnnotation = activityClass.annotations.find { it.shortName.asString() == "Az" } ?: return null
        val appArg = azAnnotation.arguments.find { it.name?.asString() == "app" }?.value as? KSAnnotation ?: return null

        var dock = "LEFT"
        var packButtons = false
        var noMenu = false
        var vibrate = false
        var displayAppName = false
        var activeClassifiers = emptyList<String>()
        var usePhysicalDocking = false
        var showFooter = true
        var expandedWidth = -1
        var collapsedWidth = -1
        var initiallyExpanded = false
        var disableSwipeToOpen = false

        appArg.arguments.forEach { arg ->
            val value = arg.value
            when (arg.name?.asString()) {
                "dock" -> dock = (value as? KSType)?.declaration?.simpleName?.asString() ?: "LEFT"
                "packButtons" -> packButtons = value as? Boolean ?: false
                "noMenu" -> noMenu = value as? Boolean ?: false
                "vibrate" -> vibrate = value as? Boolean ?: false
                "displayAppName" -> displayAppName = value as? Boolean ?: false
                "activeClassifiers" -> activeClassifiers = (value as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                "usePhysicalDocking" -> usePhysicalDocking = value as? Boolean ?: false
                "showFooter" -> showFooter = value as? Boolean ?: true
                "expandedWidth" -> expandedWidth = value as? Int ?: -1
                "collapsedWidth" -> collapsedWidth = value as? Int ?: -1
                "initiallyExpanded" -> initiallyExpanded = value as? Boolean ?: false
                "disableSwipeToOpen" -> disableSwipeToOpen = value as? Boolean ?: false
            }
        }
        return AppConfig(dock, packButtons, noMenu, vibrate, displayAppName, usePhysicalDocking, showFooter, expandedWidth, collapsedWidth, initiallyExpanded, disableSwipeToOpen, activeClassifiers)
    }

    private fun extractThemeConfig(activityClass: KSClassDeclaration): ThemeConfig? {
        val azAnnotation = activityClass.annotations.find { it.shortName.asString() == "Az" } ?: return null
        val themeArg = azAnnotation.arguments.find { it.name?.asString() == "theme" }?.value as? KSAnnotation ?: return null

        var activeColorHex = ""
        var defaultShape = "CIRCLE"
        var headerIconShape = "CIRCLE"

        themeArg.arguments.forEach { arg ->
            val value = arg.value
            when (arg.name?.asString()) {
                "activeColorHex" -> activeColorHex = value as? String ?: ""
                "defaultShape" -> defaultShape = (value as? KSType)?.declaration?.simpleName?.asString() ?: "CIRCLE"
                "headerIconShape" -> headerIconShape = (value as? KSType)?.declaration?.simpleName?.asString() ?: "CIRCLE"
            }
        }
        return ThemeConfig(activeColorHex, defaultShape, headerIconShape)
    }

    private fun extractAdvancedConfig(activityClass: KSClassDeclaration): AdvancedData? {
        val azAnnotation = activityClass.annotations.find { it.shortName.asString() == "Az" } ?: return null
        val advArg = azAnnotation.arguments.find { it.name?.asString() == "advanced" }?.value as? KSAnnotation ?: return null

        var isLoading = false
        var infoScreen = false
        var enableRailDragging = false
        var overlayServiceClass = ""
        var onUndock = ""
        var onRailDrag = ""
        var onOverlayDrag = ""
        var onItemGloballyPositioned = ""

        advArg.arguments.forEach { arg ->
            val value = arg.value
            when (arg.name?.asString()) {
                "isLoading" -> isLoading = value as? Boolean ?: false
                "infoScreen" -> infoScreen = value as? Boolean ?: false
                "enableRailDragging" -> enableRailDragging = value as? Boolean ?: false
                "overlayServiceClass" -> overlayServiceClass = value as? String ?: ""
                "onUndock" -> onUndock = value as? String ?: ""
                "onRailDrag" -> onRailDrag = value as? String ?: ""
                "onOverlayDrag" -> onOverlayDrag = value as? String ?: ""
                "onItemGloballyPositioned" -> onItemGloballyPositioned = value as? String ?: ""
            }
        }

        val (pkg, name, member) = getSymbolInfo(activityClass)
        return AdvancedData(isLoading, infoScreen, enableRailDragging, overlayServiceClass, onUndock, onRailDrag, onOverlayDrag, onItemGloballyPositioned, member, name, pkg, activityClass)
    }

    private fun extractItems(symbols: List<KSAnnotated>, activityClass: KSClassDeclaration): List<ItemData> {
        val items = mutableListOf<ItemData>()
        symbols.forEach { symbol ->
            val az = symbol.annotations.find { it.shortName.asString() == "Az" } ?: return@forEach
            az.arguments.forEach { arg ->
                val name = arg.name?.asString()
                val value = arg.value
                if (value is KSAnnotation) {
                    when (name) {
                        "rail" -> items.add(extractRailItem(symbol, value, false))
                        "menu" -> items.add(extractRailItem(symbol, value, true))
                        "host" -> items.add(extractHostItem(symbol, value))
                        "nested" -> items.add(extractNestedItem(symbol, value))
                        "toggle" -> items.add(extractToggleItem(symbol, value))
                        "cycler" -> items.add(extractCyclerItem(symbol, value))
                        "reloc" -> items.add(extractRelocItem(symbol, value))
                        "background" -> items.add(extractBackgroundItem(symbol, value))
                        "divider" -> items.add(extractDividerItem(symbol))
                    }
                }
            }
        }
        return items
    }

    private fun getSymbolInfo(symbol: KSAnnotated): Triple<String, String, Boolean> {
        val declaration = symbol as? KSDeclaration
        val packageName = declaration?.packageName?.asString() ?: ""
        val functionName = declaration?.simpleName?.asString() ?: ""
        val isMember = declaration?.parentDeclaration != null
        return Triple(packageName, functionName, isMember)
    }

    private fun String.splitCamelCase() = replace(Regex("([a-z])([A-Z]+)"), "$1 $2")
    private fun String.toSnakeCase() = replace(Regex("([a-z])([A-Z]+)"), "$1_$2").lowercase(Locale.ROOT)

    private fun extractRailItem(symbol: KSAnnotated, annotation: KSAnnotation, isMenu: Boolean): RailItemData {
        val (pkg, name, member) = getSymbolInfo(symbol)
        var id = ""
        var text = ""
        var icon = 0
        var iconText = ""
        var home = false
        var parent = ""
        var disabled = false
        var screenTitle = ""
        var info = ""
        var classifiers = emptyList<String>()
        var onFocus = ""

        annotation.arguments.forEach { arg ->
            val value = arg.value
            when (arg.name?.asString()) {
                "id" -> id = value as? String ?: ""
                "text" -> text = value as? String ?: ""
                "icon" -> icon = value as? Int ?: 0
                "iconText" -> iconText = value as? String ?: ""
                "home" -> home = value as? Boolean ?: false
                "parent" -> parent = value as? String ?: ""
                "disabled" -> disabled = value as? Boolean ?: false
                "screenTitle" -> screenTitle = value as? String ?: ""
                "info" -> info = value as? String ?: ""
                "classifiers" -> classifiers = (value as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                "onFocus" -> onFocus = value as? String ?: ""
            }
        }
        if (id.isEmpty()) id = name.toSnakeCase()
        if (text.isEmpty()) text = name.splitCamelCase()

        return RailItemData(id, parent, text, icon, iconText, false, home, isMenu, disabled, screenTitle, info, classifiers, onFocus, true, false, false, member, name, pkg, symbol)
    }

    private fun extractHostItem(symbol: KSAnnotated, annotation: KSAnnotation): RailHostData {
        val (pkg, name, member) = getSymbolInfo(symbol)
        var id = ""
        var text = ""
        var icon = 0
        var iconText = ""
        var isMenu = false
        var disabled = false
        var screenTitle = ""
        var info = ""

        annotation.arguments.forEach { arg ->
            val value = arg.value
            when (arg.name?.asString()) {
                "id" -> id = value as? String ?: ""
                "text" -> text = value as? String ?: ""
                "icon" -> icon = value as? Int ?: 0
                "iconText" -> iconText = value as? String ?: ""
                "isMenu" -> isMenu = value as? Boolean ?: false
                "disabled" -> disabled = value as? Boolean ?: false
                "screenTitle" -> screenTitle = value as? String ?: ""
                "info" -> info = value as? String ?: ""
            }
        }
        if (id.isEmpty()) id = name.toSnakeCase()
        if (text.isEmpty()) text = name.splitCamelCase()
        return RailHostData(id, text, icon, iconText, isMenu, disabled, screenTitle, info, member, name, pkg, symbol)
    }

    private fun extractNestedItem(symbol: KSAnnotated, annotation: KSAnnotation): NestedRailData {
        val (pkg, name, member) = getSymbolInfo(symbol)
        var id = ""
        var parent = ""
        var text = ""
        var icon = 0
        var iconText = ""
        var disabled = false
        var screenTitle = ""
        var info = ""
        var classifiers = emptyList<String>()
        var onFocus = ""
        var alignment = "VERTICAL"

        annotation.arguments.forEach { arg ->
            val value = arg.value
            when (arg.name?.asString()) {
                "id" -> id = value as? String ?: ""
                "parent" -> parent = value as? String ?: ""
                "text" -> text = value as? String ?: ""
                "icon" -> icon = value as? Int ?: 0
                "iconText" -> iconText = value as? String ?: ""
                "disabled" -> disabled = value as? Boolean ?: false
                "screenTitle" -> screenTitle = value as? String ?: ""
                "info" -> info = value as? String ?: ""
                "classifiers" -> classifiers = (value as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                "onFocus" -> onFocus = value as? String ?: ""
                "alignment" -> alignment = (value as? KSType)?.declaration?.simpleName?.asString() ?: "VERTICAL"
            }
        }
        if (id.isEmpty()) id = name.toSnakeCase()
        if (text.isEmpty()) text = name.splitCamelCase()
        return NestedRailData(id, parent, text, icon, iconText, disabled, screenTitle, info, classifiers, onFocus, alignment, true, false, false, member, name, pkg, symbol)
    }

    private fun extractToggleItem(symbol: KSAnnotated, annotation: KSAnnotation): ToggleData {
        val (pkg, name, member) = getSymbolInfo(symbol)
        var id = ""
        var parent = ""
        var toggleOnText = ""
        var toggleOffText = ""
        var isMenu = false
        var disabled = false
        var screenTitle = ""
        var info = ""

        annotation.arguments.forEach { arg ->
            val value = arg.value
            when (arg.name?.asString()) {
                "id" -> id = value as? String ?: ""
                "parent" -> parent = value as? String ?: ""
                "toggleOnText" -> toggleOnText = value as? String ?: ""
                "toggleOffText" -> toggleOffText = value as? String ?: ""
                "isMenu" -> isMenu = value as? Boolean ?: false
                "disabled" -> disabled = value as? Boolean ?: false
                "screenTitle" -> screenTitle = value as? String ?: ""
                "info" -> info = value as? String ?: ""
            }
        }
        if (id.isEmpty()) id = name.toSnakeCase()
        val isProperty = symbol is KSPropertyDeclaration
        val isAction = !isProperty // assuming it's either property or function (action)
        return ToggleData(id, parent, toggleOnText, toggleOffText, isMenu, disabled, screenTitle, info, false, isAction, isProperty, member, name, pkg, symbol)
    }

    private fun extractCyclerItem(symbol: KSAnnotated, annotation: KSAnnotation): CyclerData {
        val (pkg, name, member) = getSymbolInfo(symbol)
        var id = ""
        var parent = ""
        var options = emptyList<String>()
        var isMenu = false
        var disabled = false
        var disabledOptions = emptyList<String>()
        var screenTitle = ""
        var info = ""

        annotation.arguments.forEach { arg ->
            val value = arg.value
            when (arg.name?.asString()) {
                "id" -> id = value as? String ?: ""
                "parent" -> parent = value as? String ?: ""
                "options" -> options = (value as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                "isMenu" -> isMenu = value as? Boolean ?: false
                "disabled" -> disabled = value as? Boolean ?: false
                "disabledOptions" -> disabledOptions = (value as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                "screenTitle" -> screenTitle = value as? String ?: ""
                "info" -> info = value as? String ?: ""
            }
        }
        if (id.isEmpty()) id = name.toSnakeCase()
        val isProperty = symbol is KSPropertyDeclaration
        val isAction = !isProperty
        return CyclerData(id, parent, options, isMenu, disabled, disabledOptions, screenTitle, info, false, isAction, isProperty, member, name, pkg, symbol)
    }

    private fun extractRelocItem(symbol: KSAnnotated, annotation: KSAnnotation): RelocItemData {
        val (pkg, name, member) = getSymbolInfo(symbol)
        var id = ""
        var parent = ""
        var text = ""
        var icon = 0
        var iconText = ""
        var hiddenMenuRoutes = emptyList<String>()
        var hiddenMenuActions = emptyList<String>()
        var hiddenMenuInputs = emptyList<String>()
        var disabled = false
        var screenTitle = ""
        var info = ""
        var classifiers = emptyList<String>()
        var onFocus = ""
        var onRelocate = ""

        annotation.arguments.forEach { arg ->
            val value = arg.value
            when (arg.name?.asString()) {
                "id" -> id = value as? String ?: ""
                "parent" -> parent = value as? String ?: ""
                "text" -> text = value as? String ?: ""
                "icon" -> icon = value as? Int ?: 0
                "iconText" -> iconText = value as? String ?: ""
                "hiddenMenuRoutes" -> hiddenMenuRoutes = (value as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                "hiddenMenuActions" -> hiddenMenuActions = (value as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                "hiddenMenuInputs" -> hiddenMenuInputs = (value as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                "disabled" -> disabled = value as? Boolean ?: false
                "screenTitle" -> screenTitle = value as? String ?: ""
                "info" -> info = value as? String ?: ""
                "classifiers" -> classifiers = (value as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                "onFocus" -> onFocus = value as? String ?: ""
                "onRelocate" -> onRelocate = value as? String ?: ""
            }
        }
        if (id.isEmpty()) id = name.toSnakeCase()
        if (text.isEmpty()) text = name.splitCamelCase()
        val hasContent = symbol is KSFunctionDeclaration && (symbol.returnType?.resolve()?.declaration?.simpleName?.asString() == "Unit") // Assuming content functions return Unit (Composable)
        // Actually, logic for hasContent was in original code: "items.filter { it.hasContent ... }"
        // For RelocItemData, in original code it was "hasContent = true" (implied by default?)
        // Let's assume reloc items have content if they are composables.
        // But `extractRelocItem` signature implies it's a Rail Item variant.
        // Let's assume hasContent=true for reloc items unless they are just actions?
        // RelocItem usually has content (the route).
        return RelocItemData(id, parent, text, icon, iconText, hiddenMenuRoutes, hiddenMenuActions, hiddenMenuInputs, disabled, screenTitle, info, classifiers, onFocus, onRelocate, true, false, member, name, pkg, symbol)
    }

    private fun extractBackgroundItem(symbol: KSAnnotated, annotation: KSAnnotation): BackgroundData {
        val (pkg, name, member) = getSymbolInfo(symbol)
        var weight = 0
        annotation.arguments.forEach { arg ->
            if (arg.name?.asString() == "weight") weight = arg.value as? Int ?: 0
        }
        return BackgroundData(weight, member, name, pkg, symbol)
    }

    private fun extractDividerItem(symbol: KSAnnotated): DividerData {
        return DividerData(symbol)
    }

    private data class AppConfig(val dock: String, val packButtons: Boolean, val noMenu: Boolean, val vibrate: Boolean, val displayAppName: Boolean, val usePhysicalDocking: Boolean, val showFooter: Boolean, val expandedWidth: Int, val collapsedWidth: Int, val initiallyExpanded: Boolean, val disableSwipeToOpen: Boolean, val activeClassifiers: List<String>)
    private data class ThemeConfig(val activeColorHex: String, val defaultShape: String, val headerIconShape: String)
    private interface ItemData { val id: String; val parent: String; val hasContent: Boolean; val isAction: Boolean; val isProperty: Boolean; val isMember: Boolean; val functionName: String; val packageName: String; val symbol: KSNode }
    private interface MetadataItem { val disabled: Boolean; val screenTitle: String; val info: String }
    private interface IconData { val icon: Int; val iconText: String }
    private data class RailItemData(override val id: String, override val parent: String, val text: String, override val icon: Int, override val iconText: String, val isHost: Boolean, val isHome: Boolean, val isMenu: Boolean, override val disabled: Boolean, override val screenTitle: String, override val info: String, val classifiers: List<String>, val onFocus: String, override val hasContent: Boolean, override val isAction: Boolean, override val isProperty: Boolean, override val isMember: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData, MetadataItem, IconData
    private data class NestedRailData(override val id: String, override val parent: String, val text: String, override val icon: Int, override val iconText: String, override val disabled: Boolean, override val screenTitle: String, override val info: String, val classifiers: List<String>, val onFocus: String, val alignment: String, override val hasContent: Boolean, override val isAction: Boolean, override val isProperty: Boolean, override val isMember: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData, MetadataItem, IconData
    private data class RailHostData(override val id: String, val text: String, override val icon: Int, override val iconText: String, val isMenu: Boolean, override val disabled: Boolean, override val screenTitle: String, override val info: String, override val isMember: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData, MetadataItem, IconData { override val hasContent = false; override val isAction = false; override val isProperty = false; override val parent = "" }
    private data class BackgroundData(val weight: Int, override val isMember: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData { override val id = ""; override val parent = ""; override val hasContent = true; override val isAction = false; override val isProperty = false }
    private data class ToggleData(override val id: String, override val parent: String, val toggleOnText: String, val toggleOffText: String, val isMenu: Boolean, override val disabled: Boolean, override val screenTitle: String, override val info: String, override val hasContent: Boolean, override val isAction: Boolean, override val isProperty: Boolean, override val isMember: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData, MetadataItem
    private data class CyclerData(override val id: String, override val parent: String, val options: List<String>, val isMenu: Boolean, override val disabled: Boolean, val disabledOptions: List<String>, override val screenTitle: String, override val info: String, override val hasContent: Boolean, override val isAction: Boolean, override val isProperty: Boolean, override val isMember: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData, MetadataItem
    private data class RelocItemData(override val id: String, override val parent: String, val text: String, override val icon: Int, override val iconText: String, val hiddenMenuRoutes: List<String>, val hiddenMenuActions: List<String>, val hiddenMenuInputs: List<String>, override val disabled: Boolean, override val screenTitle: String, override val info: String, val classifiers: List<String>, val onFocus: String, val onRelocate: String, override val hasContent: Boolean, override val isAction: Boolean, override val isMember: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData, MetadataItem, IconData { override val isProperty = false }
    private data class DividerData(override val symbol: KSNode) : ItemData { override val id = ""; override val parent = ""; override val hasContent = false; override val isAction = false; override val isProperty = false; override val isMember = false; override val functionName = ""; override val packageName = "" }
    private data class AdvancedData(val isLoading: Boolean, val infoScreen: Boolean, val enableRailDragging: Boolean, val overlayServiceClass: String, val onUndock: String, val onRailDrag: String, val onOverlayDrag: String, val onItemGloballyPositioned: String, val isMember: Boolean, val functionName: String, val packageName: String, val symbol: KSNode)
}
