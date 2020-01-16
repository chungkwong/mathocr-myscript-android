![Demo](docs/demo.gif?raw=true)

# Offline handwritten mathematical expression recognition via stroke extraction for Android

The repository provides a proof-of-concept stroke extractor that can extract strokes from clean
bitmap images. The stroke extractor can be used to recognize offline handwritten
mathematical expression if a online recognizer is given. For example, when combined
with MyScript, the resulting offline recognition system was **ranked #3 in the offline
task in CROHME 2019.**

## Accuracy

Dataset|Correct|Up to 1 error|Up to 2 errors|Structural correct
---|---|---|---|---
CROHME 2014|58.22%|71.60%|75.15%|77.38%
CROHME 2016|65.65%|77.68%|82.56%|85.00%
CROHME 2019|65.22%|78.48%|83.07%|84.90%

Although good accuracy is achieved on datasets of CROHME, the program
may produce poor results on real world images. For example, the procedure do not
work well on the following images:
- Images containing other objects. An image should contain exactly one formula and nothing else.
Ordinary text and grid lines are not allowed.
- Images with low contrast. The strokes may not be distinguished from background properly.
- Images with low resolution. The stroke extractor may not segment touching symbols correctly.
- Printed mathematical expressions. Serifs can distract the stroke extractor.

## Build from source

1. Clone this repository: `git clone 'https://github.com/chungkwong/mathocr-myscript-android.git`
2. Log into your MyScript [dashboard](https://developer.myscript.com/dashboard)
3. Manage `On-device recognition(Free trial)`
4. Create an application with any name and description you like
5. Create a certificate with identifier `cc.chungkwong.mathocr` for `Android`
6. Download the certificate just created to `offlinerecognizer/src/main/java/cc/chungkwong/mathocr/MyCertificate.java`
7. Change the first line of that file to `package cc.chungkwong.mathocr;`
8. Generate signed APK for the module `offlinerecognizer` using Android Studio or command line

## Usage

1. Install the APK just built
2. Launch the application `MathOCR`
3. Choose the image to be recognized:
    - Click the button `File` and then choose a file
    - Click the button `Camera` and then take a photo
4. Click the button `Recognize` and the recognized LaTeX code will show at the bottom
5. If you need MathML in stead of LaTeX, choose `MathML` from the menu

## Citation

The idea used is explained in the article
__Stroke extraction for offline handwritten mathematical expression recognition__
, which is available at [arXiv](https://arxiv.org/abs/1905.06749).
You can cite the article using the following BibTex code:

```bibtex
@misc{1905.06749,
Author = {Chungkwong Chan},
Title = {Stroke extraction for offline handwritten mathematical expression recognition},
Year = {2019},
Eprint = {arXiv:1905.06749},
}
```

# 基于笔划提取的脱机手写数学公式识别

本项目提供一个可从清晰的图片中还原笔划信息的程序原型。与联机手写数学公式识别结合的话，
可以打造出脱机数学公式识别系统。例如与MyScript结合时 **在CROHME 2019的脱机任务中位列第3名**。

## 准确度

数据集|正确|至多一处错误|至多两处错误|结构正确
---|---|---|---|---
CROHME 2014|58.22%|71.60%|75.15%|77.38%
CROHME 2016|65.65%|77.68%|82.56%|85.00%
CROHME 2019|65.22%|78.48%|83.07%|84.90%

虽然在CROHME数据集上取得了良好的表现，本程序对现实世界中的图片表现往往未如理想。
例如对以下类型的图片可能给出差劲的结果：

- 含有其它对象的图片。图片中只应含有一条公式而没有其它东西，不能有普通文本或网格之类。
- 低对比度图片。这时笔划难以从背景区分出来。
- 低清晰度图片。这时粘连在一起的符号难以分割。
- 印刷体数学公式。衬线会干扰笔划提取。

## 从源构建

1. 克隆本仓库：`git clone 'https://github.com/chungkwong/mathocr-myscript-android.git`
2. 登录你的MyScript [控制台](https://developer.myscript.com/dashboard)
3. 管理`On-device recognition(Free trial)`
4. 创建一个应用，名字和描述随意
5. 创建一张标识符为`cc.chungkwong.mathocr`的`Android`证书
6. 把证书下载到`offlinerecognizer/src/main/java/cc/chungkwong/mathocr/MyCertificate.java`
7. 把该文件首行改为`package cc.chungkwong.mathocr;`
8. 用Android Studio或命令行为模块`offlinerecognizer`创建并签署APK

## 用法

1. 安装用上述方法构建的APK
2. 启动应用`公式识别`
3. 选择要识别的图片:
    - 点击`文件`后选取一个图片文件
    - 点击`拍照`后拍照
4. 点击`识别`后LaTeX代码会显示在下方
5. 如果你需要MathML而非LaTeX，在菜单中选择`MathML`

## 引用

本项目的描述参见文档 __通过笔划提取识别脱机手写数学公式__，它可从
[arXiv](https://arxiv.org/abs/1905.06749)下载。你可以使用以下BibTex代码引用该文:

```bibtex
@misc{1905.06749,
Author = {Chungkwong Chan},
Title = {Stroke extraction for offline handwritten mathematical expression recognition},
Year = {2019},
Eprint = {arXiv:1905.06749},
}
```
