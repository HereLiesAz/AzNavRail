# The Great Purge: Migrating to High-Inference

**Current Status:** `Legacy Boilerplate` ➔ **Target:** `High-Inference Dictatorship`

This guide details how to migrate your application from the manual `AzHostActivityLayout` configuration to the automated `@Az` annotation system.

The "New Way" is not a suggestion. It is the architectural standard. It eliminates 95% of the setup code and enforces layout compliance at compile time.

---

## 1. Dependency Updates

You must install the **Processor** (The Brains) and the **Annotations** (The Signals).

**`app/build.gradle.kts`**

~~~kotlin
plugins {
    // 1. Add KSP (Must match your Kotlin version)
    id("com.google.devtools.ksp") version "2.0.0-1.0.21" 
}

dependencies {
    implementation("com.github.HereLiesAz.AzNavRail:aznavrail:VERSION")
    
    // 2. Add the High-Inference Modules
    implementation("com.github.HereLiesAz.AzNavRail:aznavrail-annotation:VERSION")
    ksp("com.github.HereLiesAz.AzNavRail:aznavrail-processor:VERSION")
}
~~~

---

## 2. The Activity Coup

**Goal:** Eliminate `setContent`, `AzHostActivityLayout`, and manual `AzNavHost` construction.

**Legacy (The Old Way):**
~~~kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { 
            val navController = rememberNavController()
            AzHostActivityLayout(navController = navController) {
                azTheme(...)
                azConfig(...)
                azRailItem(...)
                onscreen { ... }
            }
        }
    }
}
~~~

**High-Inference (The New Way):**
~~~kotlin
// 1. Configuration moves to the Annotation
@Az(app = App(dock = AzDockingSide.LEFT, theme = AzTheme.GlitchNoir))
class MainActivity : AzActivity() {
    // 2. Point to the Generated Graph
    override val graph = AzGraph 
}
~~~

---

## 3. The Station Migration

**Goal:** Convert manual `azRailItem` calls into annotated Composables.

**Legacy (The Old Way):**
*Defined inside the Activity DSL:*
~~~kotlin
azRailItem(id = "home", text = "Home", route = "home")
// ...
composable("home") { HomeScreen() }
~~~

**High-Inference (The New Way):**
*Defined on the Function itself:*
~~~kotlin
@Az(rail = RailItem(home = true)) // ID="home", Text="Home" inferred
@Composable
fun Home() { 
    // Content 
}
~~~

---

## 4. Hierarchy Migration

**Goal:** Convert `azRailHostItem` and `azNestedRail`.

**Legacy:**
~~~kotlin
azRailHostItem(id = "settings", text = "Settings")
azRailSubItem(parent = "settings", id = "wifi", text = "WiFi", route = "wifi")
~~~

**High-Inference:**
~~~kotlin
// Hosts are now Properties (since they have no screen content)
@Az(railHost = RailHost) 
val Settings = null 

// Children link via 'parent'
@Az(rail = RailItem(parent = "settings"))
@Composable 
fun Wifi() { ... }
~~~

---

## 5. Automated Migration Script

Don't want to rewrite manually? We have provided a script to perform a hostile takeover of your codebase.

1.  Copy `scripts/migrate_to_az.py` to your project root.
2.  Run: `python3 migrate_to_az.py ./app/src/main/java`
3.  The script will:
    * Scan for usages of `azRailItem`, `azMenuItem`, etc.
    * Extract IDs, Text, and Routes.
    * Generate a new file `AzMigratedSpecs.kt` containing the equivalent `@Az` annotated functions.
4.  Copy the generated code into your project and delete the old configuration.
