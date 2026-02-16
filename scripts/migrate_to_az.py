import os
import re
import argparse

# The Dictator's Migration Tool
# Scans Kotlin files for legacy DSL calls and generates @Az annotated replacements.

def scan_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Regex patterns to capture the Legacy DSL parameters
    # Matches: azRailItem(id = "foo", text = "Bar", ...)
    rail_pattern = re.compile(r'azRailItem\s*\((.*?)\)', re.DOTALL)
    menu_pattern = re.compile(r'azMenuItem\s*\((.*?)\)', re.DOTALL)
    host_pattern = re.compile(r'azRailHostItem\s*\((.*?)\)', re.DOTALL)
    sub_pattern = re.compile(r'azRailSubItem\s*\((.*?)\)', re.DOTALL)

    items = []

    # Helper to parse arguments into a dict
    def parse_args(args_str):
        args = {}
        # Naive split by comma, works for simple strings/booleans
        parts = args_str.split(',')
        for part in parts:
            if '=' in part:
                k, v = part.split('=', 1)
                args[k.strip()] = v.strip().replace('"', '')
        return args

    # 1. Scan Rail Items
    for match in rail_pattern.finditer(content):
        args = parse_args(match.group(1))
        items.append({
            'type': 'rail',
            'id': args.get('id', 'unknown_id'),
            'text': args.get('text', 'Unknown'),
            'route': args.get('route', None)
        })

    # 2. Scan Menu Items
    for match in menu_pattern.finditer(content):
        args = parse_args(match.group(1))
        items.append({
            'type': 'menu',
            'id': args.get('id', 'unknown_id'),
            'text': args.get('text', 'Unknown'),
            'route': args.get('route', None)
        })

    # 3. Scan Hosts
    for match in host_pattern.finditer(content):
        args = parse_args(match.group(1))
        items.append({
            'type': 'host',
            'id': args.get('id', 'unknown_id'),
            'text': args.get('text', 'Unknown')
        })

    # 4. Scan Sub Items
    for match in sub_pattern.finditer(content):
        args = parse_args(match.group(1))
        items.append({
            'type': 'sub',
            'parent': args.get('hostId', 'unknown_parent'),
            'id': args.get('id', 'unknown_id'),
            'text': args.get('text', 'Unknown'),
            'route': args.get('route', None)
        })

    return items

def generate_kotlin_code(items):
    output = []
    output.append("package com.hereliesaz.migrated")
    output.append("")
    output.append("import androidx.compose.runtime.Composable")
    output.append("import androidx.compose.material3.Text")
    output.append("import com.hereliesaz.aznavrail.annotation.*")
    output.append("")
    output.append("// AUTO-GENERATED MIGRATION FILE")
    output.append("// Copy these functions to your project and delete the old DSL setup.")
    output.append("")

    for item in items:
        clean_id = item['id'].replace('-', '_').replace(' ', '_')
        func_name = "".join(x.capitalize() for x in clean_id.split('_'))
        
        output.append(f"// Original ID: {item['id']}")
        
        if item['type'] == 'host':
            # Hosts become properties
            output.append(f"@Az(railHost = RailHost(id = \"{item['id']}\", text = \"{item['text']}\"))")
            output.append(f"val {func_name}Host = null")
        
        elif item['type'] == 'sub':
            # Sub items link to parent
            output.append(f"@Az(rail = RailItem(id = \"{item['id']}\", text = \"{item['text']}\", parent = \"{item['parent']}\"))")
            output.append("@Composable")
            output.append(f"fun {func_name}Screen() {{")
            output.append(f"    Text(\"Migrated Screen: {item['text']}\")")
            output.append("}")

        elif item['type'] == 'menu':
             # Menu items
            output.append(f"@Az(menu = MenuItem(id = \"{item['id']}\", text = \"{item['text']}\"))")
            output.append("@Composable")
            output.append(f"fun {func_name}Screen() {{")
            output.append(f"    Text(\"Migrated Screen: {item['text']}\")")
            output.append("}")

        else: # Rail Item
            output.append(f"@Az(rail = RailItem(id = \"{item['id']}\", text = \"{item['text']}\"))")
            output.append("@Composable")
            output.append(f"fun {func_name}Screen() {{")
            output.append(f"    Text(\"Migrated Screen: {item['text']}\")")
            output.append("}")
        
        output.append("")

    return "\n".join(output)

def main():
    parser = argparse.ArgumentParser(description='Migrate AzNavRail DSL to @Az Annotations')
    parser.add_argument('directory', help='Directory to scan for Kotlin files')
    args = parser.parse_args()

    all_items = []

    print(f"Scanning {args.directory} for legacy AzNavRail usage...")

    for root, dirs, files in os.walk(args.directory):
        for file in files:
            if file.endswith(".kt"):
                path = os.path.join(root, file)
                items = scan_file(path)
                if items:
                    print(f"Found {len(items)} items in {file}")
                    all_items.extend(items)

    if not all_items:
        print("No legacy usage found. You are either clean or using a format I can't regex.")
        return

    migrated_code = generate_kotlin_code(all_items)
    
    output_file = "AzMigratedSpecs.kt"
    with open(output_file, 'w') as f:
        f.write(migrated_code)

    print(f"\nSUCCESS. Generated {len(all_items)} migrated stations.")
    print(f"Code written to: {os.path.abspath(output_file)}")
    print("Action Required: Copy the contents of this file into your project sources.")

if __name__ == "__main__":
    main()
