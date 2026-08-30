import os
import re

base_dir = '/home/sumit/Documents/GitHub/TTS-Website/backend/src/main/java/com/voisetu/backend'
new_dirs = ['controller', 'repository', 'model', 'security', 'client']

for d in new_dirs:
    os.makedirs(os.path.join(base_dir, d), exist_ok=True)

moves = {
    'AdminController.java': 'controller',
    'AnalyticsController.java': 'controller',
    'AuthController.java': 'controller',
    'ContactController.java': 'controller',
    'GenerationController.java': 'controller',
    'HistoryController.java': 'controller',
    'InterestController.java': 'controller',
    'ProjectController.java': 'controller',
    'PublicStatsController.java': 'controller',
    'SiteMetricsController.java': 'controller',
    'TtsController.java': 'controller',
    'UsageController.java': 'controller',
    'UserController.java': 'controller',
    'VoiceController.java': 'controller',
    
    'AppUserRepository.java': 'repository',
    'DashboardRepository.java': 'repository',
    
    'AppUser.java': 'model',
    
    'CustomUserDetailsService.java': 'security',
    'JwtAuthFilter.java': 'security',
    'JwtService.java': 'security',
    'SecurityConfig.java': 'security',
    
    'SupertonicClient.java': 'client'
}

for file_name, pkg in moves.items():
    src = os.path.join(base_dir, file_name)
    dst = os.path.join(base_dir, pkg, file_name)
    if os.path.exists(src):
        os.rename(src, dst)

class_to_new_pkg = {}
for file_name, pkg in moves.items():
    class_name = file_name.replace('.java', '')
    class_to_new_pkg[class_name] = f"com.voisetu.backend.{pkg}"

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    original_content = content
    
    rel_path = os.path.relpath(filepath, base_dir)
    parts = rel_path.split(os.sep)
    if len(parts) > 1 and parts[0] in new_dirs:
        new_pkg = f"com.voisetu.backend.{parts[0]}"
        content = re.sub(r'^package com\.voisetu\.backend;$', f"package {new_pkg};", content, flags=re.MULTILINE)
    
    for cls, pkg in class_to_new_pkg.items():
        old_import = f"import com.voisetu.backend.{cls};"
        new_import = f"import {pkg}.{cls};"
        
        # if the old explicit import is there, replace it
        content = content.replace(old_import, new_import)
        
        # if the class is used and there's no new_import
        # we might need to add it, unless they are in the same package
        if re.search(r'\b' + cls + r'\b', content) and not new_import in content:
            current_pkg_match = re.search(r'^package (.+);', content, flags=re.MULTILINE)
            current_pkg = current_pkg_match.group(1) if current_pkg_match else "com.voisetu.backend"
            if current_pkg != pkg:
                # Add import right after the package declaration
                content = re.sub(r'^(package .+;)', f"\\1\n\n{new_import}", content, count=1, flags=re.MULTILINE)

    if original_content != content:
        with open(filepath, 'w') as f:
            f.write(content)

for root, dirs, files in os.walk(base_dir):
    for f in files:
        if f.endswith('.java'):
            process_file(os.path.join(root, f))
