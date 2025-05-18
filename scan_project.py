#!/usr/bin/env python3

import os
import re
import json
from pathlib import Path

def find_files_by_extension(directory, extension):
    """Find all files with the given extension in the directory and subdirectories."""
    return list(Path(directory).rglob(f"*.{extension}"))

def find_main_class():
    """Find the main class file containing main method."""
    java_files = find_files_by_extension(".", "java")
    main_classes = []
    
    for file_path in java_files:
        with open(file_path, 'r', encoding='utf-8', errors='ignore') as file:
            content = file.read()
            # Look for public static void main
            if re.search(r'public\s+static\s+void\s+main', content):
                main_classes.append(str(file_path))
                
    return main_classes

def find_parser_classes():
    """Find CSV parser related classes."""
    java_files = find_files_by_extension(".", "java")
    parser_classes = []
    
    keywords = ["CSV", "Parser", "parse", "killfeed"]
    
    for file_path in java_files:
        with open(file_path, 'r', encoding='utf-8', errors='ignore') as file:
            content = file.read()
            # Check if any of the keywords appear in the file
            if any(keyword.lower() in content.lower() for keyword in keywords):
                parser_classes.append(str(file_path))
                
    return parser_classes

def find_config_files():
    """Find configuration files."""
    config_files = []
    
    # Common config file patterns
    config_files.extend(find_files_by_extension(".", "properties"))
    config_files.extend(find_files_by_extension(".", "yml"))
    config_files.extend(find_files_by_extension(".", "yaml"))
    config_files.extend(find_files_by_extension(".", "json"))
    config_files.extend(find_files_by_extension(".", "xml"))
    
    # Check for .env file
    if os.path.exists(".env"):
        config_files.append(".env")
    
    return [str(file) for file in config_files]

def find_event_command_files():
    """Find event handlers and command files."""
    java_files = find_files_by_extension(".", "java")
    event_command_files = []
    
    keywords = ["Command", "Event", "SlashCommand", "EventListener", "MessageReceived"]
    
    for file_path in java_files:
        with open(file_path, 'r', encoding='utf-8', errors='ignore') as file:
            content = file.read()
            # Check if any of the keywords appear in the file
            if any(keyword.lower() in content.lower() for keyword in keywords):
                event_command_files.append(str(file_path))
                
    return event_command_files

def find_duplicate_files():
    """Find potential duplicate files by name."""
    java_files = find_files_by_extension(".", "java")
    file_names = {}
    duplicates = []
    
    for file_path in java_files:
        file_name = file_path.name
        if file_name in file_names:
            duplicates.append((str(file_names[file_name]), str(file_path)))
        else:
            file_names[file_name] = file_path
            
    return duplicates

def find_orphaned_files():
    """Find potentially orphaned files (not imported anywhere)."""
    java_files = find_files_by_extension(".", "java")
    class_names = {}
    imported_classes = set()
    
    # Extract class names first
    for file_path in java_files:
        with open(file_path, 'r', encoding='utf-8', errors='ignore') as file:
            content = file.read()
            # Extract class or interface name
            match = re.search(r'(public|private)\s+(class|interface|enum)\s+(\w+)', content)
            if match:
                class_name = match.group(3)
                class_names[class_name] = str(file_path)
    
    # Now check imports
    for file_path in java_files:
        with open(file_path, 'r', encoding='utf-8', errors='ignore') as file:
            content = file.read()
            # Find all import statements
            for match in re.finditer(r'import\s+(?:static\s+)?([^;]+);', content):
                import_path = match.group(1)
                # Extract the class name from the import statement
                import_class = import_path.split('.')[-1]
                if import_class in class_names:
                    imported_classes.add(import_class)
    
    # Find orphaned classes (those not imported anywhere)
    orphaned = []
    for class_name, file_path in class_names.items():
        if class_name not in imported_classes:
            # Skip main classes as they're not typically imported
            main_classes = find_main_class()
            if file_path not in main_classes:
                orphaned.append(file_path)
    
    return orphaned

def scan_project():
    """Scan the project and return the results."""
    results = {
        "main_classes": find_main_class(),
        "parser_classes": find_parser_classes(),
        "config_files": find_config_files(),
        "event_command_files": find_event_command_files(),
        "duplicate_files": find_duplicate_files(),
        "orphaned_files": find_orphaned_files()
    }
    
    return results

if __name__ == "__main__":
    print("--- PROJECT STRUCTURE SCAN ---")
    
    results = scan_project()
    
    print("\n=== MAIN CLASS ===")
    if results["main_classes"]:
        for main_class in results["main_classes"]:
            print(f"- {main_class}")
    else:
        print("No main class found! The bot may not start correctly.")
    
    print("\n=== PARSER CLASSES ===")
    if results["parser_classes"]:
        for parser_class in results["parser_classes"]:
            print(f"- {parser_class}")
    else:
        print("No parser classes found.")
    
    print("\n=== CONFIG FILES ===")
    if results["config_files"]:
        for config_file in results["config_files"]:
            print(f"- {config_file}")
    else:
        print("No config files found. A new one will be created.")
    
    print("\n=== EVENT/COMMAND SYSTEM FILES ===")
    if results["event_command_files"]:
        for event_file in results["event_command_files"]:
            print(f"- {event_file}")
    else:
        print("No event/command files found.")
    
    print("\n=== POTENTIAL DUPLICATE FILES ===")
    if results["duplicate_files"]:
        for file1, file2 in results["duplicate_files"]:
            print(f"- {file1} and {file2}")
    else:
        print("No duplicate files found.")
    
    print("\n=== POTENTIAL ORPHANED FILES ===")
    if results["orphaned_files"]:
        for orphaned in results["orphaned_files"]:
            print(f"- {orphaned}")
    else:
        print("No orphaned files found.")
    
    print("\n--- SCAN COMPLETE ---")
    
    # Save results to a file for reference
    with open("project_scan_results.json", "w") as f:
        json.dump(results, f, indent=2)
    
    print("Results saved to project_scan_results.json")
