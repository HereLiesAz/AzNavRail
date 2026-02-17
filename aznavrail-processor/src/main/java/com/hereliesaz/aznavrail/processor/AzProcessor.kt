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

        // Validation check removed to prevent circular dependency with generated graph
        val activityClass = symbols.filterIsInstance<KSClassDeclaration>().firstOrNull() ?: return emptyList()

        val packageName = activityClass.packageName.asString()
        val graphClassName = "AzGraph"

        val fileSpec = FileSpec.builder(packageName, graphClassName)
            .addImport("androidx.compose.runtime", "Composable")
            .addImport("androidx.activity.compose", "setContent")
            .addImport("androidx.navigation.compose", "rememberNavController", "composable")
            .addImport("com.hereliesaz.aznavrail", "AzHostActivityLayout", "AzNavHost", "AzGraphInterface")
            .addImport("com.hereliesaz.aznavrail.model", "AzDockingSide", "AzNestedRailAlignment")
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

        if (appConfig.dock != null) {
            builder.add("azConfig(\n")
            builder.indent()
            builder.addStatement("dockingSide = %T.%L,", ClassName("com.hereliesaz.aznavrail.model", "AzDockingSide"), appConfig.dock)
            builder.unindent()
            builder.addStatement(")\n")
        }

        // Group items: Top-level vs Children (SubItems or NestedItems)
        val topLevelItems = mutableListOf<ItemData>()
        val childrenMap = mutableMapOf<String, MutableList<ItemData>>()

        items.forEach { item ->
            if (item.parent.isNotEmpty()) {
                childrenMap.getOrPut(item.parent) { mutableListOf() }.add(item)
            } else {
                topLevelItems.add(item)
            }
        }

        topLevelItems.forEach { item ->
            generateItem(builder, item, childrenMap)
        }

        builder.addStatement("")
        val contentItems = items.filter { it.hasContent }
        
        // Fix: Detect 'home = true'
        val homeItem = items.filterIsInstance<RailItemData>().find { it.isHome && it.hasContent }
        val startDest = homeItem?.id ?: contentItems.firstOrNull()?.id ?: "home"

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
                // Standard Top-Level Rail Item
                builder.add("azRailItem(\n")
                builder.indent()
                builder.addStatement("id = %S,", item.id)
                if (item.icon != 0) builder.addStatement("content = %L,", item.icon)
                if (item.text.isNotEmpty()) builder.addStatement("text = %S,", item.text)
                if (item.hasContent) {
                    builder.addStatement("route = %S,", item.id)
                } else {
                    builder.addStatement("onClick = {},")
                }
                builder.unindent()
                builder.addStatement(")")
            }
            is NestedRailData -> {
                // Parenting: A Nested Rail "parents" children
                builder.add("azNestedRail(\n")
                builder.indent()
                builder.addStatement("id = %S,", item.id)
                if (item.text.isNotEmpty()) builder.addStatement("text = %S,", item.text)
                 builder.addStatement("alignment = %T.VERTICAL,", ClassName("com.hereliesaz.aznavrail.model", "AzNestedRailAlignment"))
                builder.unindent()
                builder.beginControlFlow(")")

                // Generate children recursively inside the block
                childrenMap[item.id]?.forEach { child ->
                     generateItem(builder, child, childrenMap)
                }

                builder.endControlFlow()
            }
            is RailHostData -> {
                 // Hosting: A Rail Host "hosts" sub-items inline
                 builder.add("azRailHostItem(\n")
                builder.indent()
                builder.addStatement("id = %S,", item.id)
                if (item.text.isNotEmpty()) builder.addStatement("text = %S,", item.text)
                builder.addStatement("onClick = {},")
                builder.unindent()
                builder.addStatement(")")

                 // Generate children as SubItems (Siblings in DSL, but logically children)
                 childrenMap[item.id]?.forEach { child ->
                     // If child is a RailItem, it becomes a SubItem
                     if (child is RailItemData) {
                          builder.add("azRailSubItem(\n")
                          builder.indent()
                          builder.addStatement("id = %S,", child.id)
                          builder.addStatement("hostId = %S,", item.id)
                          if (child.text.isNotEmpty()) builder.addStatement("text = %S,", child.text)
                          if (child.hasContent) {
                              builder.addStatement("route = %S,", child.id)
                          } else {
                              builder.addStatement("onClick = {},")
                          }
                          builder.unindent()
                          builder.addStatement(")")
                     }
                }
            }
        }
    }

    private data class AppConfig(val dock: String?)
    
    // Interface includes 'parent' to handle hierarchy
    private interface ItemData { 
        val id: String
        val parent: String 
        val hasContent: Boolean
        val functionName: String
        val packageName: String
        val symbol: KSNode 
    }
    
    private data class RailItemData(
        override val id: String, 
        override val parent: String,
        val text: String, 
        val icon: Int, 
        val isHost: Boolean, 
        val isHome: Boolean, 
        override val hasContent: Boolean, 
        override val functionName: String, 
        override val packageName: String, 
        override val symbol: KSNode
    ) : ItemData
    
    private data class NestedRailData(
        override val id: String, 
        override val parent: String,
        val text: String, 
        val icon: Int, 
        override val hasContent: Boolean, 
        override val functionName: String, 
        override val packageName: String, 
        override val symbol: KSNode
    ) : ItemData
    
    private data class RailHostData(
        override val id: String, 
        val text: String, 
        val icon: Int, 
        override val functionName: String, 
        override val packageName: String, 
        override val symbol: KSNode
    ) : ItemData { 
        override val hasContent = false 
        override val parent = "" // Hosts are typically top-level
    }

    private fun extractAppConfig(activity: KSClassDeclaration): AppConfig {
        val azAnnot = activity.getAnnotation("com.hereliesaz.aznavrail.annotation.Az") ?: return AppConfig(null)
        val appAnnot = azAnnot.getArgument("app") as? KSAnnotation ?: return AppConfig(null)

        val dockArg = appAnnot.arguments.find { it.name?.asString() == "dock" }?.value
        val dock = (dockArg as? KSType)?.declaration?.simpleName?.asString() ?: dockArg?.toString()?.substringAfterLast(".")

        return AppConfig(dock)
    }

    private fun extractItems(symbols: List<KSAnnotated>, activityClass: KSClassDeclaration): List<ItemData> {
        val items = mutableListOf<ItemData>()

        symbols.forEach { symbol ->
            if (symbol is KSFunctionDeclaration || symbol is KSPropertyDeclaration) {
                val azAnnot = symbol.getAnnotation("com.hereliesaz.aznavrail.annotation.Az") ?: return@forEach

                if (symbol is KSFunctionDeclaration) {
                    val isComposable = symbol.annotations.any {
                        it.shortName.asString() == "Composable" ||
                        it.annotationType.resolve().declaration.qualifiedName?.asString() == "androidx.compose.runtime.Composable"
                    }
                    if (!isComposable) {
                        logger.error("Function '${symbol.simpleName.asString()}' annotated with @Az must be @Composable.", symbol)
                        return@forEach 
                    }
                }

                val railAnnot = azAnnot.getArgument("rail") as? KSAnnotation
                val hostAnnot = azAnnot.getArgument("host") as? KSAnnotation
                val nestedAnnot = azAnnot.getArgument("nested") as? KSAnnotation

                val name = symbol.simpleName.asString()
                val pkg = symbol.packageName.asString()
                val hasContent = symbol is KSFunctionDeclaration 

                val inferredId = name.toSnakeCase()
                val inferredText = name.splitCamelCase()

                val isRail = railAnnot?.getArgument("isValid") as? Boolean ?: false
                val isHost = hostAnnot?.getArgument("isValid") as? Boolean ?: false
                val isNested = nestedAnnot?.getArgument("isValid") as? Boolean ?: false

                if (isRail) {
                    items.add(RailItemData(
                        id = (railAnnot!!.getArgument("id") as? String)?.takeIf { it.isNotEmpty() } ?: inferredId,
                        parent = (railAnnot.getArgument("parent") as? String) ?: "",
                        text = (railAnnot.getArgument("text") as? String)?.takeIf { it.isNotEmpty() } ?: inferredText,
                        icon = (railAnnot.getArgument("icon") as? Int) ?: 0,
                        isHost = (railAnnot.getArgument("isHost") as? Boolean) ?: false,
                        isHome = (railAnnot.getArgument("home") as? Boolean) ?: false,
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
