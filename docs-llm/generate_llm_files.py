#!/usr/bin/env python3

import os
import glob
from datetime import datetime
from pathlib import Path

"""Should run this script against chromia-docs repository"""


def priority_sort_key(path):
    """Sort key to prioritize index and overview files within each directory."""
    dir_path = os.path.dirname(path)
    filename = os.path.basename(path).lower()

    priority = 2
    if filename in ("index.md", "index.mdx"):
        priority = 0
    elif filename in ("overview.md", "overview.mdx"):
        priority = 1

    return (dir_path, priority, path)


def collect_markdown_files(directory):
    """Recursively collect all .md and .mdx files in a directory"""
    md_files = []

    for pattern in ["**/*.md", "**/*.mdx"]:
        md_files.extend(glob.glob(os.path.join(directory, pattern), recursive=True))

    return sorted(md_files, key=priority_sort_key)


def process_base_directory(base_dir_path):
    if not os.path.exists(base_dir_path) or not os.path.isdir(base_dir_path):
        print(f"Directory {base_dir_path} does not exist or is not a directory")
        return

    base_md_files = []
    for file in os.listdir(base_dir_path):
        file_path = os.path.join(base_dir_path, file)
        if os.path.isfile(file_path) and file.lower().endswith((".md", ".mdx")):
            base_md_files.append(file_path)

    if base_md_files:
        base_name = os.path.basename(base_dir_path)
        output_filename = f"llm_{base_name.replace('-', '_')}_root.txt"
        create_llm_file(
            base_md_files, output_filename, f"{base_name} (root files)", base_dir_path
        )

    subdirectories = [
        d
        for d in os.listdir(base_dir_path)
        if os.path.isdir(os.path.join(base_dir_path, d))
    ]
    subdirectories.sort()

    for subdir_name in subdirectories:
        subdir_path = os.path.join(base_dir_path, subdir_name)
        output_filename = f"llm_{subdir_name.replace('-', '_')}.txt"

        print(f"Processing directory: {subdir_path} -> {output_filename}")

        md_files = collect_markdown_files(subdir_path)

        create_llm_file(md_files, output_filename, subdir_name.upper(), subdir_path)


def create_llm_file(md_files, output_filename, section_name, source_path):
    """Create an LLM file with the given markdown files"""
    content = []
    content.append(f"# CHROMIA {section_name} DOCUMENTATION\n")
    content.append(f"Generated on: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
    content.append(f"Source directory: {source_path}\n\n")
    content.append("=" * 80 + "\n\n")

    if md_files:
        for file_path in md_files:
            relative_path = os.path.relpath(file_path, ".")
            # content.append(f"## File: {relative_path}\n\n")

            try:
                with open(file_path, "r", encoding="utf-8") as f:
                    lines = f.readlines()

                new_lines = []
                i = 0
                while i < len(lines):
                    if (
                        i + 2 < len(lines)
                        and lines[i].strip() == "---"
                        and "sidebar_position:" in lines[i + 1].strip()
                        and lines[i + 2].strip() == "---"
                    ):
                        i += 3
                    else:
                        new_lines.append(lines[i])
                        i += 1
                file_content = "".join(new_lines)
                content.append(file_content)
                content.append("\n\n")
                content.append("-" * 80 + "\n\n")
            except Exception as e:
                print(f"Error reading file: {e}")

    try:
        with open(output_filename, "w", encoding="utf-8") as f:
            f.write("".join(content))
        print(f"Created: {output_filename} ({len(md_files)} files processed)")
    except Exception as e:
        print(f"Error writing {output_filename}: {e}")


def process_network_config():
    """Process network_config directory for JSON files"""
    network_config_dir = "network_config"

    if not os.path.exists(network_config_dir) or not os.path.isdir(network_config_dir):
        print("network_config directory does not exist")
        return

    output_filename = "llm_network_config.txt"
    print(f"Processing network_config directory -> {output_filename}")

    content = []
    content.append("# CHROMIA NETWORK CONFIGURATION FILES\n\n")
    content.append(f"Generated on: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
    content.append("Source directory: network_config\n\n")
    content.append("=" * 80 + "\n\n")

    json_files = []
    for file in os.listdir(network_config_dir):
        file_path = os.path.join(network_config_dir, file)
        if os.path.isfile(file_path) and file.lower().endswith(".json"):
            json_files.append(file_path)

    json_files.sort()

    if json_files:
        for file_path in json_files:
            filename = os.path.basename(file_path)
            # content.append(f"## File: {filename}\n\n")

            try:
                with open(file_path, "r", encoding="utf-8") as f:
                    file_content = f.read()
                content.append("```json\n")
                content.append(file_content)
                content.append("\n```\n\n")
                content.append("-" * 80 + "\n\n")
            except Exception as e:
                print(f"Error reading file: {e}\n\n")

    try:
        with open(output_filename, "w", encoding="utf-8") as f:
            f.write("".join(content))
        print(f"Created: {output_filename} ({len(json_files)} files processed)")
    except Exception as e:
        print(f"Error writing {output_filename}: {e}")


def process_static_pages():
    """Process static/pages directory with special handling"""
    static_pages_dir = "static/pages"
    if not os.path.exists(static_pages_dir) or not os.path.isdir(static_pages_dir):
        print("static/pages directory does not exist")
        return

    base_files = []
    for file in os.listdir(static_pages_dir):
        file_path = os.path.join(static_pages_dir, file)
        if os.path.isfile(file_path) and (
            file.lower().endswith((".md", ".mdx", ".yaml", ".yml"))
        ):
            base_files.append(file_path)

    if base_files:
        base_files.sort(key=priority_sort_key)
        create_static_pages_file(
            base_files,
            "llm_static_pages_root.txt",
            "STATIC PAGES (root files)",
            static_pages_dir,
        )

    subdirectories = [
        d
        for d in os.listdir(static_pages_dir)
        if os.path.isdir(os.path.join(static_pages_dir, d))
    ]
    subdirectories.sort()

    for subdir_name in subdirectories:
        subdir_path = os.path.join(static_pages_dir, subdir_name)
        output_filename = f"llm_static_{subdir_name.replace('-', '_')}.txt"

        print(f"Processing static/pages directory: {subdir_path} -> {output_filename}")

        files = []
        for pattern in ["**/*.md", "**/*.mdx", "**/*.yaml", "**/*.yml"]:
            files.extend(glob.glob(os.path.join(subdir_path, pattern), recursive=True))

        files.sort(key=priority_sort_key)
        create_static_pages_file(
            files, output_filename, f"STATIC {subdir_name.upper()}", subdir_path
        )


def create_static_pages_file(files, output_filename, section_name, source_path):
    content = []
    content.append(f"# CHROMIA {section_name} DOCUMENTATION\n\n")
    content.append(f"Generated on: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
    content.append(f"Source directory: {source_path}\n\n")
    content.append("=" * 80 + "\n\n")

    if files:
        for file_path in files:
            relative_path = os.path.relpath(file_path, ".")
            # content.append(f"## File: {relative_path}\n\n")

            try:
                with open(file_path, "r", encoding="utf-8") as f:
                    file_content_str = f.read()

                ext = os.path.splitext(file_path)[1].lower()
                if ext in [".md", ".mdx"]:
                    lang = None
                    lines = file_content_str.splitlines(True)
                    new_lines = []
                    i = 0
                    while i < len(lines):
                        if (
                            i + 2 < len(lines)
                            and lines[i].strip() == "---"
                            and "sidebar_position:" in lines[i + 1].strip()
                            and lines[i + 2].strip() == "---"
                        ):
                            i += 3
                        else:
                            new_lines.append(lines[i])
                            i += 1
                    file_content_str = "".join(new_lines)

                elif ext in [".yaml", ".yml"]:
                    lang = "yaml"
                else:
                    lang = "text"

                if lang:
                    content.append(f"```{lang}\n")
                content.append(file_content_str)
                if lang:
                    content.append("\n```")
                content.append("\n\n")
                content.append("-" * 80 + "\n\n")
            except Exception as e:
                print(f"Error reading file: {e}\n\n")
                print("-" * 80 + "\n\n")

    try:
        with open(output_filename, "w", encoding="utf-8") as f:
            f.write("".join(content))
        print(f"Created: {output_filename} ({len(files)} files processed)")
    except Exception as e:
        print(f"Error writing {output_filename}: {e}")


def main():
    """Main function to process all directories"""
    base_directories = ["docs", "generated"]

    for base_dir in base_directories:
        process_base_directory(base_dir)

    process_static_pages()

    process_network_config()

    print("LLM files generation completed!")


if __name__ == "__main__":
    main()
