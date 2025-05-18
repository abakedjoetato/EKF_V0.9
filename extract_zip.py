import zipfile
import shutil
import os

# Extract the zip file
with zipfile.ZipFile('attached_assets/EFKBeta1-main.zip', 'r') as zip_ref:
    zip_ref.extractall('.')

# Print the extracted contents
print("Extracted contents:")
os.system('ls -la')

# Move all contents from the extracted directory to the root if needed
extracted_dir = 'EFKBeta1-main'
if os.path.exists(extracted_dir) and os.path.isdir(extracted_dir):
    print(f"\nMoving contents from {extracted_dir} to root...")
    for item in os.listdir(extracted_dir):
        src = os.path.join(extracted_dir, item)
        dst = os.path.join('.', item)
        if os.path.exists(dst):
            if os.path.isdir(dst):
                # Merge directories
                for subitem in os.listdir(src):
                    shutil.move(os.path.join(src, subitem), dst)
            else:
                # For files, replace if source is newer
                if os.path.getmtime(src) > os.path.getmtime(dst):
                    shutil.move(src, dst)
        else:
            shutil.move(src, dst)
    
    # Clean up the extracted directory
    if os.path.exists(extracted_dir) and not os.listdir(extracted_dir):
        os.rmdir(extracted_dir)
        print(f"Removed empty directory: {extracted_dir}")

# Show final structure
print("\nFinal directory structure:")
os.system('ls -la')
print("\nDetailed structure of src directory (if exists):")
if os.path.exists('src'):
    os.system('find src -type f | sort')