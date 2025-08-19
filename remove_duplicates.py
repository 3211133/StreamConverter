#!/usr/bin/env python3

import os
import re

# Files that need cleaning
files = [
    "streamconverter-core/src/test/java/com/streamConverter/command/impl/XmlNavigateCommandTest.java",
    "streamconverter-core/src/test/java/com/streamConverter/command/impl/charaCode/ConvertTest.java",
    "streamconverter-core/src/test/java/com/streamConverter/command/impl/JsonNavigateCommandTest.java",
    "streamconverter-core/src/test/java/com/streamConverter/command/impl/CsvNavigateCommandTest.java",
    "streamconverter-core/src/test/java/com/streamConverter/command/impl/SampleStreamCommandTest.java",
    "streamconverter-core/src/test/java/com/streamConverter/command/impl/xml/ConvertCommandTest.java",
    "streamconverter-core/src/test/java/com/streamConverter/command/impl/LineEndingNormalizeCommandTest.java",
    "streamconverter-core/src/test/java/com/streamConverter/command/impl/csv/CsvValidateCommandTest.java",
    "streamconverter-core/src/test/java/com/streamConverter/command/impl/json/JsonValidateCommandTest.java"
]

def add_imports_and_remove_duplicates(file_path):
    if not os.path.exists(file_path):
        print(f"File not found: {file_path}")
        return
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Add imports if not already present
    if "import com.streamConverter.test.StreamingTestUtils.TrackingInputStream;" not in content:
        content = re.sub(
            r'(import.*?;\n)(\n.*?class)',
            r'\1import com.streamConverter.test.StreamingTestUtils.TrackingInputStream;\n\2',
            content,
            flags=re.DOTALL
        )
    
    if "MonitoringOutputStream" in content and "import com.streamConverter.test.StreamingTestUtils.MonitoringOutputStream;" not in content:
        content = re.sub(
            r'(import.*?;\n)(\n.*?class)',
            r'\1import com.streamConverter.test.StreamingTestUtils.MonitoringOutputStream;\n\2',
            content,
            flags=re.DOTALL
        )
    
    # Remove TrackingInputStream class
    content = re.sub(
        r'\s*\/\*\* Custom InputStream.*?\*\/\s*private static class TrackingInputStream.*?^\s*}\s*$',
        '',
        content,
        flags=re.MULTILINE | re.DOTALL
    )
    
    # Remove MonitoringOutputStream class
    content = re.sub(
        r'\s*\/\*\* Custom OutputStream.*?\*\/\s*private static class MonitoringOutputStream.*?^\s*}\s*$',
        '',
        content,
        flags=re.MULTILINE | re.DOTALL
    )
    
    # Clean up extra whitespace
    content = re.sub(r'\n\s*\n\s*\n', '\n\n', content)
    content = re.sub(r'\n\s*}\s*$', '\n}\n', content)
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f"Processed: {file_path}")

for file_path in files:
    add_imports_and_remove_duplicates(file_path)

print("All files processed!")