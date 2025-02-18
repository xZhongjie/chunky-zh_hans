import os
from concurrent.futures import ThreadPoolExecutor, as_completed

script_dir = os.path.dirname(os.path.abspath(__file__))

#在此修改
file_specific_replacements = {
    os.path.join(script_dir, r'.\chunky\src\java\se\llbit\chunky\ui\render\tabs\LightingTab.java'): {
        "Emitter intensity": "测试用例",
        "Modifies the intensity of emitter light.": "测试用例2"
    },
}

def apply_replacements(file_path, replacements):
    if not os.path.isfile(file_path):
        print(f"警告: 文件 {file_path} 不存在，跳过处理。")
        return
    
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            content = file.read()
        
        for old_str, new_str in replacements.items():
            content = content.replace(old_str, new_str)
        
        with open(file_path, 'w', encoding='utf-8') as file:
            file.write(content)
        
        print(f"已处理文件: {file_path}")
    except Exception as e:
        print(f"处理文件 {file_path} 时出错: {e}")

def process_files_with_specific_rules(file_specific_replacements):
    with ThreadPoolExecutor() as executor:
        futures = [
            executor.submit(apply_replacements, file_path, replacements)
            for file_path, replacements in file_specific_replacements.items()
        ]
        
        for future in as_completed(futures):
            future.result()

if __name__ == "__main__":
    process_files_with_specific_rules(file_specific_replacements)