// aznavrail-processor/src/main/java/com/hereliesaz/aznavrail/processor/AzProcessor.kt
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

        val activityClass = symbols.filterIsInstance<KSClassDeclaration>().firstOrNull() ?: return emptyList()

        val packageName = activityClass.packageName.asString()
        val graphClassName = "AzGraph"

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
            .addCode(generateRunBody(activityClass, symbols))

        graphObject.addFunction(runFunction.build())
        fileSpec.addType(graphObject.build())

        fileSpec.build().writeTo(codeGenerator, Dependencies(true, *symbols.mapNotNull { (it as? KSDeclaration)?.containingFile }.toTypedArray()))

        isGenerated = true
        return emptyList()
    }

    private fun generateRunBody(activityClass: KSClassDeclaration, symbols: List<KSAnnotated>): CodeBlock {
        val appConfig = extractAppConfig(activityClass)
        val advancedConfig = extractAdvancedConfig(activityClass)
        val items = extractItems(symbols, activityClass)

        val builder = CodeBlock.builder()

        builder.beginControlFlow("activity.setContent")
        builder.addStatement("val navController = rememberNavController()")

        val backgroundItems = items.filterIsInstance<BackgroundData>()
        val contentItems = items.filter { it.hasContent && it !is BackgroundData }
        val homeItem = items.filterIsInstance<RailItemData>().find { it.isHome && it.hasContent }
        val startDest = homeItem?.id ?: contentItems.firstOrNull()?.id ?: "home"

        builder.add("AzHostActivityLayout(\n")
        builder.indent()
        builder.addStatement("navController = navController,")
        builder.unindent()
        builder.add(") {\n")
        builder.indent()

        backgroundItems.forEach { bg ->
            builder.beginControlFlow("background(weight = %L)", bg.weight)
            builder.addStatement("%M()", MemberName(bg.packageName, bg.functionName))
            builder.endControlFlow()
        }

        builder.add("onscreen {\n")
        builder.indent()
        builder.beginControlFlow("AzNavHost(startDestination = %S)", startDest)

        contentItems.forEach { item ->
            builder.beginControlFlow("composable(%S)", item.id)
            val funcName = item.functionName
            val pkg = item.packageName
            builder.addStatement("%M()", MemberName(pkg, funcName))
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
                val classStr = appConfig.activeClassifiers.joinToString(", ") { "\"$it\"" }
                builder.addStatement("activeClassifiers = setOf(%L),", classStr)
            }
            builder.unindent()
            builder.addStatement(")\n")
        }

        if (advancedConfig != null) {
            builder.add("azAdvanced(\n")
            builder.indent()
            if (advancedConfig.isLoading) {
                val loadingProp = MemberName(advancedConfig.packageName, advancedConfig.functionName)
                builder.addStatement("isLoading = %M,", loadingProp)
            }
            if (advancedConfig.infoScreen) {
                val infoProp = MemberName(advancedConfig.packageName, advancedConfig.functionName)
                builder.addStatement("infoScreen = %M,", infoProp)
                builder.addStatement("onDismissInfoScreen = { %M = false },", infoProp)
            }
            if (advancedConfig.enableRailDragging) {
                builder.addStatement("enableRailDragging = true,")
            }
            if (advancedConfig.overlayServiceClass.isNotEmpty()) {
                val serviceClass = ClassName.bestGuess(advancedConfig.overlayServiceClass)
                builder.addStatement("overlayService = %T::class.java,", serviceClass)
            }
            builder.unindent()
            builder.addStatement(")\n")
        }

        builder.addStatement("val azActivity = activity as? %T", ClassName("com.hereliesaz.aznavrail", "AzActivity"))
        builder.addStatement("azActivity?.apply { configureRail() }")
        builder.addStatement("")

        val topLevelItems = mutableListOf<ItemData>()
        val childrenMap = mutableMapOf<String, MutableList<ItemData>>()

        items.filter { it !is BackgroundData && it !is AdvancedData }.forEach { item ->
            if (item.parent.isNotEmpty()) {
                childrenMap.getOrPut(item.parent) { mutableListOf() }.add(item)
            } else {
                topLevelItems.add(item)
            }
        }

        topLevelItems.forEach { item ->
            generateItem(builder, item, childrenMap)
        }

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
                if (item.icon != 0 && !item.isMenu) builder.addStatement("content = %L,", item.icon)
                if (item.text.isNotEmpty()) builder.addStatement("text = %S,", item.text)
                addMetadataParameters(builder, item)
                if (!item.isMenu && item.classifiers.isNotEmpty()) {
                    builder.addStatement("classifiers = setOf(%L),", item.classifiers.joinToString(", ") { "\"$it\"" })
                }
                
                if (item.hasContent) {
                    builder.addStatement("route = %S,", item.id)
                } else if (item.isAction) {
                    builder.addStatement("onClick = { %M() },", MemberName(item.packageName, item.functionName))
                } else {
                    builder.addStatement("onClick = {},")
                }
                builder.unindent()
                builder.addStatement(")")
            }
            is NestedRailData -> {
                builder.add("azNestedRail(\n")
                builder.indent()
                builder.addStatement("id = %S,", item.id)
                if (item.text.isNotEmpty()) builder.addStatement("text = %S,", item.text)
                addMetadataParameters(builder, item)
                if (item.classifiers.isNotEmpty()) {
                    builder.addStatement("classifiers = setOf(%L),", item.classifiers.joinToString(", ") { "\"$it\"" })
                }
                builder.addStatement("alignment = %T.VERTICAL,", ClassName("com.hereliesaz.aznavrail.model", "AzNestedRailAlignment"))
                builder.unindent()
                builder.beginControlFlow(")")

                childrenMap[item.id]?.forEach { child ->
                     generateItem(builder, child, childrenMap)
                }

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
                          if (!item.isMenu && child.classifiers.isNotEmpty()) {
                              builder.addStatement("classifiers = setOf(%L),", child.classifiers.joinToString(", ") { "\"$it\"" })
                          }
                          
                          if (child.hasContent) {
                              builder.addStatement("route = %S,", child.id)
                          } else if (child.isAction) {
                              builder.addStatement("onClick = { %M() },", MemberName(child.packageName, child.functionName))
                          } else {
                              builder.addStatement("onClick = {},")
                          }
                          builder.unindent()
                          builder.addStatement(")")
                     } else if (child is ToggleData || child is CyclerData || child is RelocItemData) {
                          generateItem(builder, child, childrenMap)
                     }
                }
            }
            is ToggleData -> {
                val prefix = when {
                    item.parent.isNotEmpty() && item.isMenu -> "azMenuSubToggle"
                    item.parent.isNotEmpty() && !item.isMenu -> "azRailSubToggle"
                    item.isMenu -> "azMenuToggle"
                    else -> "azRailToggle"
                }
                builder.add("$prefix(\n")
                builder.indent()
                builder.addStatement("id = %S,", item.id)
                if (item.parent.isNotEmpty()) builder.addStatement("hostId = %S,", item.parent)
                builder.addStatement("toggleOnText = %S,", item.toggleOnText)
                builder.addStatement("toggleOffText = %S,", item.toggleOffText)
                addMetadataParameters(builder, item)

                if (item.isProperty) {
                    val prop = MemberName(item.packageName, item.functionName)
                    builder.addStatement("isChecked = %M,", prop)
                    builder.addStatement("onClick = { %M = !%M },", prop, prop)
                } else if (item.isAction) {
                    builder.addStatement("isChecked = false,")
                    builder.addStatement("onClick = { %M() },", MemberName(item.packageName, item.functionName))
                } else {
                    builder.addStatement("isChecked = false,")
                    builder.addStatement("onClick = {},")
                }
                builder.unindent()
                builder.addStatement(")")
            }
            is CyclerData -> {
                val prefix = when {
                    item.parent.isNotEmpty() && item.isMenu -> "azMenuSubCycler"
                    item.parent.isNotEmpty() && !item.isMenu -> "azRailSubCycler"
                    item.isMenu -> "azMenuCycler"
                    else -> "azRailCycler"
                }
                builder.add("$prefix(\n")
                builder.indent()
                builder.addStatement("id = %S,", item.id)
                if (item.parent.isNotEmpty()) builder.addStatement("hostId = %S,", item.parent)
                val optionsStr = item.options.joinToString(", ") { "\"$it\"" }
                builder.addStatement("options = listOf(%L),", optionsStr)
                addMetadataParameters(builder, item)
                if (item.disabledOptions.isNotEmpty()) {
                     builder.addStatement("disabledOptions = listOf(%L),", item.disabledOptions.joinToString(", ") { "\"$it\"" })
                }
                
                if (item.isProperty) {
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
                } else if (item.isAction) {
                    val firstOpt = item.options.firstOrNull() ?: ""
                    builder.addStatement("selectedOption = %S,", firstOpt)
                    builder.addStatement("onClick = { %M() },", MemberName(item.packageName, item.functionName))
                } else {
                    val firstOpt = item.options.firstOrNull() ?: ""
                    builder.addStatement("selectedOption = %S,", firstOpt)
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
                if (item.classifiers.isNotEmpty()) {
                    builder.addStatement("classifiers = setOf(%L),", item.classifiers.joinToString(", ") { "\"$it\"" })
                }
                
                if (item.hasContent) {
                    builder.addStatement("route = %S,", item.id)
                } else if (item.isAction) {
                    builder.addStatement("onClick = { %M() },", MemberName(item.packageName, item.functionName))
                } else {
                    builder.addStatement("onClick = {},")
                }
                builder.unindent()
                builder.beginControlFlow(") {")
                item.hiddenMenuRoutes.forEach { route ->
                    builder.addStatement("listItem(%S, %S)", route.splitCamelCase(), route)
                }
                builder.endControlFlow()
            }
            is DividerData -> {
                builder.addStatement("azDivider()")
            }
            else -> {}
        }
    }
    
    private fun addMetadataParameters(builder: CodeBlock.Builder, item: MetadataItem) {
        if (item.disabled) builder.addStatement("disabled = true,")
        if (item.screenTitle.isNotEmpty()) builder.addStatement("screenTitle = %S,", item.screenTitle)
        if (item.info.isNotEmpty()) builder.addStatement("info = %S,", item.info)
    }

    private data class AppConfig(val dock: String, val packButtons: Boolean, val noMenu: Boolean, val vibrate: Boolean, val displayAppName: Boolean, val usePhysicalDocking: Boolean, val showFooter: Boolean, val expandedWidth: Int, val collapsedWidth: Int, val activeClassifiers: List<String>)
    
    private interface ItemData { val id: String; val parent: String; val hasContent: Boolean; val isAction: Boolean; val isProperty: Boolean; val functionName: String; val packageName: String; val symbol: KSNode }
    private interface MetadataItem { val disabled: Boolean; val screenTitle: String; val info: String }
    
    private data class RailItemData(override val id: String, override val parent: String, val text: String, val icon: Int, val isHost: Boolean, val isHome: Boolean, val isMenu: Boolean, override val disabled: Boolean, override val screenTitle: String, override val info: String, val classifiers: List<String>, override val hasContent: Boolean, override val isAction: Boolean, override val isProperty: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData, MetadataItem
    private data class NestedRailData(override val id: String, override val parent: String, val text: String, val icon: Int, override val disabled: Boolean, override val screenTitle: String, override val info: String, val classifiers: List<String>, override val hasContent: Boolean, override val isAction: Boolean, override val isProperty: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData, MetadataItem
    private data class RailHostData(override val id: String, val text: String, val icon: Int, val isMenu: Boolean, override val disabled: Boolean, override val screenTitle: String, override val info: String, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData, MetadataItem { override val hasContent = false; override val isAction = false; override val isProperty = false; override val parent = "" }
    private data class BackgroundData(val weight: Int, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData { override val id = ""; override val parent = ""; override val hasContent = true; override val isAction = false; override val isProperty = false }
    private data class ToggleData(override val id: String, override val parent: String, val toggleOnText: String, val toggleOffText: String, val isMenu: Boolean, override val disabled: Boolean, override val screenTitle: String, override val info: String, override val hasContent: Boolean, override val isAction: Boolean, override val isProperty: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData, MetadataItem
    private data class CyclerData(override val id: String, override val parent: String, val options: List<String>, val isMenu: Boolean, override val disabled: Boolean, val disabledOptions: List<String>, override val screenTitle: String, override val info: String, override val hasContent: Boolean, override val isAction: Boolean, override val isProperty: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData, MetadataItem
    private data class RelocItemData(override val id: String, override val parent: String, val text: String, val hiddenMenuRoutes: List<String>, override val disabled: Boolean, override val screenTitle: String, override val info: String, val classifiers: List<String>, override val hasContent: Boolean, override val isAction: Boolean, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData, MetadataItem { override val isProperty = false }
    private data class DividerData(override val symbol: KSNode) : ItemData { override val id = ""; override val parent = ""; override val hasContent = false; override val isAction = false; override val isProperty = false; override val functionName = ""; override val packageName = "" }
    private data class AdvancedData(val isLoading: Boolean, val infoScreen: Boolean, val enableRailDragging: Boolean, val overlayServiceClass: String, override val functionName: String, override val packageName: String, override val symbol: KSNode) : ItemData { override val id = ""; override val parent = ""; override val hasContent = false; override val isAction = false; override val isProperty = true }

    private fun extractAppConfig(activity: KSClassDeclaration): AppConfig? {
        val azAnnot = activity.getAnnotation("com.hereliesaz.aznavrail.annotation.Az") ?: return null
        val appAnnot = azAnnot.getArgument("app") as? KSAnnotation ?: return null
        
        val dockArg = appAnnot.arguments.find { it.name?.asString() == "dock" }?.value
        val dock = (dockArg as? KSType)?.declaration?.simpleName?.asString() ?: dockArg?.toString()?.substringAfterLast(".") ?: "LEFT"
        
        val classifiersRaw = appAnnot.getArgument("activeClassifiers") as? ArrayList<*>
        
        return AppConfig(
            dock = dock,
            packButtons = (appAnnot.getArgument("packButtons") as? Boolean) ?: false,
            noMenu = (appAnnot.getArgument("noMenu") as? Boolean) ?: false,
            vibrate = (appAnnot.getArgument("vibrate") as? Boolean) ?: false,
            displayAppName = (appAnnot.getArgument("displayAppName") as? Boolean) ?: false,
            usePhysicalDocking = (appAnnot.getArgument("usePhysicalDocking") as? Boolean) ?: false,
            showFooter = (appAnnot.getArgument("showFooter") as? Boolean) ?: true,
            expandedWidth = (appAnnot.getArgument("expandedWidth") as? Int) ?: -1,
            collapsedWidth = (appAnnot.getArgument("collapsedWidth") as? Int) ?: -1,
            activeClassifiers = classifiersRaw?.map { it.toString() } ?: emptyList()
        )
    }

    private fun extractAdvancedConfig(activity: KSClassDeclaration): AdvancedData? {
        val azAnnot = activity.getAnnotation("com.hereliesaz.aznavrail.annotation.Az") ?: return null
        val advAnnot = azAnnot.getArgument("advanced") as? KSAnnotation ?: return null
        
        val isValid = (advAnnot.getArgument("isValid") as? Boolean) ?: false
        if (!isValid) return null
        
        return AdvancedData(
            isLoading = (advAnnot.getArgument("isLoading") as? Boolean) ?: false,
            infoScreen = (advAnnot.getArgument("infoScreen") as? Boolean) ?: false,
            enableRailDragging = (advAnnot.getArgument("enableRailDragging") as? Boolean) ?: false,
            overlayServiceClass = (advAnnot.getArgument("overlayServiceClass") as? String) ?: "",
            functionName = "",
            packageName = "",
            symbol = activity
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
                val advAnnot = azAnnot.getArgument("advanced") as? KSAnnotation
                
                val name = symbol.simpleName.asString()
                val pkg = symbol.packageName.asString()
                
                val isComposable = symbol.annotations.any { it.shortName.asString() == "Composable" }
                val isFunction = symbol is KSFunctionDeclaration
                val isProperty = symbol is KSPropertyDeclaration
                val hasContent = isFunction && isComposable
                val isAction = isFunction && !isComposable
                
                val inferredId = name.toSnakeCase()
                val inferredText = name.splitCamelCase()
                
                val isRail = railAnnot?.getArgument("isValid") as? Boolean ?: false
                val isMenu = menuAnnot?.getArgument("isValid") as? Boolean ?: false
                val isHost = hostAnnot?.getArgument("isValid") as? Boolean ?: false
                val isNested = nestedAnnot?.getArgument("isValid") as? Boolean ?: false
                val isBg = bgAnnot?.getArgument("isValid") as? Boolean ?: false
                val isToggle = toggleAnnot?.getArgument("isValid") as? Boolean ?: false
                val isCycler = cyclerAnnot?.getArgument("isValid") as? Boolean ?: false
                val isReloc = relocAnnot?.getArgument("isValid") as? Boolean ?: false
                val isDivider = dividerAnnot?.getArgument("isValid") as? Boolean ?: false
                val isAdv = advAnnot?.getArgument("isValid") as? Boolean ?: false

                if (isRail || isMenu) {
                    val activeAnnot = if (isRail) railAnnot!! else menuAnnot!!
                    val classRaw = activeAnnot.getArgument("classifiers") as? ArrayList<*>
                    items.add(RailItemData(
                        id = (activeAnnot.getArgument("id") as? String)?.takeIf { it.isNotEmpty() } ?: inferredId,
                        parent = (activeAnnot.getArgument("parent") as? String) ?: "",
                        text = (activeAnnot.getArgument("text") as? String)?.takeIf { it.isNotEmpty() } ?: inferredText,
                        icon = (activeAnnot.getArgument("icon") as? Int) ?: 0,
                        isHost = false,
                        isHome = if (isRail) (activeAnnot.getArgument("home") as? Boolean) ?: false else false,
                        isMenu = isMenu,
                        disabled = (activeAnnot.getArgument("disabled") as? Boolean) ?: false,
                        screenTitle = (activeAnnot.getArgument("screenTitle") as? String) ?: "",
                        info = (activeAnnot.getArgument("info") as? String) ?: "",
                        classifiers = classRaw?.map { it.toString() } ?: emptyList(),
                        hasContent = hasContent,
                        isAction = isAction,
                        isProperty = isProperty,
                        functionName = name,
                        packageName = pkg,
                        symbol = symbol
                    ))
                } else if (isHost) {
                     items.add(RailHostData(
                        id = (hostAnnot!!.getArgument("id") as? String)?.takeIf { it.isNotEmpty() } ?: inferredId,
                        text = (hostAnnot.getArgument("text") as? String)?.takeIf { it.isNotEmpty() } ?: inferredText,
                        icon = (hostAnnot.getArgument("icon") as? Int) ?: 0,
                        isMenu = (hostAnnot.getArgument("isMenu") as? Boolean) ?: false,
                        disabled = (hostAnnot.getArgument("disabled") as? Boolean) ?: false,
                        screenTitle = (hostAnnot.getArgument("screenTitle") as? String) ?: "",
                        info = (hostAnnot.getArgument("info") as? String) ?: "",
                        functionName = name,
                        packageName = pkg,
                        symbol = symbol
                    ))
                } else if (isNested) {
                    val classRaw = nestedAnnot!!.getArgument("classifiers") as? ArrayList<*>
                    items.add(NestedRailData(
                        id = (nestedAnnot.getArgument("id") as? String)?.takeIf { it.isNotEmpty() } ?: inferredId,
                        parent = (nestedAnnot.getArgument("parent") as? String) ?: "",
                        text = (nestedAnnot.getArgument("text") as? String)?.takeIf { it.isNotEmpty() } ?: inferredText,
                        icon = (nestedAnnot.getArgument("icon") as? Int) ?: 0,
                        disabled = (nestedAnnot.getArgument("disabled") as? Boolean) ?: false,
                        screenTitle = (nestedAnnot.getArgument("screenTitle") as? String) ?: "",
                        info = (nestedAnnot.getArgument("info") as? String) ?: "",
                        classifiers = classRaw?.map { it.toString() } ?: emptyList(),
                        hasContent = hasContent,
                        isAction = isAction,
                        isProperty = isProperty,
                        functionName = name,
                        packageName = pkg,
                        symbol = symbol
                    ))
                } else if (isBg) {
                    items.add(BackgroundData(
                        weight = (bgAnnot!!.getArgument("weight") as? Int) ?: 0,
                        functionName = name,
                        packageName = pkg,
                        symbol = symbol
                    ))
                } else if (isToggle) {
                    items.add(ToggleData(
                        id = (toggleAnnot!!.getArgument("id") as? String)?.takeIf { it.isNotEmpty() } ?: inferredId,
                        parent = (toggleAnnot.getArgument("parent") as? String) ?: "",
                        toggleOnText = (toggleAnnot.getArgument("toggleOnText") as? String) ?: "On",
                        toggleOffText = (toggleAnnot.getArgument("toggleOffText") as? String) ?: "Off",
                        isMenu = (toggleAnnot.getArgument("isMenu") as? Boolean) ?: false,
                        disabled = (toggleAnnot.getArgument("disabled") as? Boolean) ?: false,
                        screenTitle = (toggleAnnot.getArgument("screenTitle") as? String) ?: "",
                        info = (toggleAnnot.getArgument("info") as? String) ?: "",
                        hasContent = hasContent,
                        isAction = isAction,
                        isProperty = isProperty,
                        functionName = name,
                        packageName = pkg,
                        symbol = symbol
                    ))
                } else if (isCycler) {
                    val rawOptions = cyclerAnnot!!.getArgument("options") as? ArrayList<*>
                    val disOptions = cyclerAnnot.getArgument("disabledOptions") as? ArrayList<*>
                    items.add(CyclerData(
                        id = (cyclerAnnot.getArgument("id") as? String)?.takeIf { it.isNotEmpty() } ?: inferredId,
                        parent = (cyclerAnnot.getArgument("parent") as? String) ?: "",
                        options = rawOptions?.map { it.toString() } ?: emptyList(),
                        isMenu = (cyclerAnnot.getArgument("isMenu") as? Boolean) ?: false,
                        disabled = (cyclerAnnot.getArgument("disabled") as? Boolean) ?: false,
                        disabledOptions = disOptions?.map { it.toString() } ?: emptyList(),
                        screenTitle = (cyclerAnnot.getArgument("screenTitle") as? String) ?: "",
                        info = (cyclerAnnot.getArgument("info") as? String) ?: "",
                        hasContent = hasContent,
                        isAction = isAction,
                        isProperty = isProperty,
                        functionName = name,
                        packageName = pkg,
                        symbol = symbol
                    ))
                } else if (isReloc) {
                    val rawRoutes = relocAnnot!!.getArgument("hiddenMenuRoutes") as? ArrayList<*>
                    val classRaw = relocAnnot.getArgument("classifiers") as? ArrayList<*>
                    items.add(RelocItemData(
                        id = (relocAnnot.getArgument("id") as? String)?.takeIf { it.isNotEmpty() } ?: inferredId,
                        parent = (relocAnnot.getArgument("parent") as? String) ?: "",
                        text = (relocAnnot.getArgument("text") as? String)?.takeIf { it.isNotEmpty() } ?: inferredText,
                        hiddenMenuRoutes = rawRoutes?.map { it.toString() } ?: emptyList(),
                        disabled = (relocAnnot.getArgument("disabled") as? Boolean) ?: false,
                        screenTitle = (relocAnnot.getArgument("screenTitle") as? String) ?: "",
                        info = (relocAnnot.getArgument("info") as? String) ?: "",
                        classifiers = classRaw?.map { it.toString() } ?: emptyList(),
                        hasContent = hasContent,
                        isAction = isAction,
                        functionName = name,
                        packageName = pkg,
                        symbol = symbol
                    ))
                } else if (isDivider) {
                    items.add(DividerData(symbol))
                } else if (isAdv) {
                    items.add(AdvancedData(
                        isLoading = (advAnnot!!.getArgument("isLoading") as? Boolean) ?: false,
                        infoScreen = (advAnnot.getArgument("infoScreen") as? Boolean) ?: false,
                        enableRailDragging = (advAnnot.getArgument("enableRailDragging") as? Boolean) ?: false,
                        overlayServiceClass = (advAnnot.getArgument("overlayServiceClass") as? String) ?: "",
                        functionName = name,
                        packageName = pkg,
                        symbol = symbol
                    ))
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
