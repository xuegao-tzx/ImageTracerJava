# ImageTracerJava
ImageTracerJava(A library that can transfer pictures such as PNG to SVG)<br>
ImageTracerJava(一个可以把png等图片转svg的开源Java库)

## How to Use:

### Including in Java projects

Add **ImageTracer.jar** to your build path<br>
把 **ImageTracer.jar** 添加到构建路径<br>

### 安装教程

**Add the mavenCentral repository under the project's build.gradle**<br>
**在 Project 的 build.gradle 下添加 mavenCentral 仓库**<br>

```groovy
repositories {
    maven {
         url 'https://repo.huaweicloud.com/repository/maven/'
    }
    maven {
        url 'https://developer.huawei.com/repo/'
    }
    mavenCentral()
}
```

**Add ImageTracerJava dependency under Module's build.gradle**<br>
**在 Module 的 build.gradle 下添加 ImageTracerJava 依赖**<br>

```groovy
implementation 'top.xuegao-tzx:ImageTracerJava:1.1.4.516'
```

then use the static methods:<br>
然后使用静态方法：<br>

```java
import com.xcl.imagetracer_mod.ImageTracer;

...
ImageTracer.saveString(
        "output.svg" ,
        ImageTracer.imageToSVG("input.jpg",null,null)
);
```

With options and palette<br>
带有选项和调色板的方法：<br>

```java
// Options
HashMap<String,Float> options = new HashMap<String,Float>();

// Tracing
options.put("ltres",1f);
options.put("qtres",1f);
options.put("pathomit",8f);

// Color quantization
options.put("colorsampling",1f); // 1f means true ; 0f means false: starting with generated palette
options.put("numberofcolors",16f);
options.put("mincolorratio",0.02f);
options.put("colorquantcycles",3f);

// SVG rendering
options.put("scale",1f);
options.put("roundcoords",1f); // 1f means rounded to 1 decimal places, like 7.3 ; 3f means rounded to 3 places, like 7.356 ; etc.
options.put("lcpr",0f);
options.put("qcpr",0f);
options.put("desc",1f); // 1f means true ; 0f means false: SVG descriptions deactivated
options.put("viewbox",0f); // 1f means true ; 0f means false: fixed width and height

// Selective Gauss Blur
options.put("blurradius",0f); // 0f means deactivated; 1f .. 5f : blur with this radius
options.put("blurdelta",20f); // smaller than this RGB difference will be blurred

// Palette
// This is an example of a grayscale palette
// please note that signed byte values [ -128 .. 127 ] will be converted to [ 0 .. 255 ] in the getsvgstring function
// the two number '8' below,you can change it to any number between 4 and 16,you need to change this by myself,so that you can make the SVG more clear!         
byte[][] palette = new byte[8][4];
for(int colorcnt=0; colorcnt < 8; colorcnt++){
	palette[colorcnt][0] = (byte)( -128 + colorcnt * 32); // R
	palette[colorcnt][1] = (byte)( -128 + colorcnt * 32); // G
	palette[colorcnt][2] = (byte)( -128 + colorcnt * 32); // B
	palette[colorcnt][3] = (byte)127; 		      // A
}

ImageTracer.saveString(
				"output.svg" ,
				ImageTracer.imageToSVG("input.jpg",options,palette)
);
```

### Deterministic output
See [options for deterministic tracing](https://github.com/jankovicsandras/imagetracerjava/blob/master/deterministic.md)


### Main Functions<br>
### 主要功能<br>
|Function name|Arguments|Returns|
|-------------|---------|-------|
|```imageToSVG```|```String filename, HashMap<String,Float> options /*can be null*/, byte [][] palette /*can be null*/```|```String /*SVG content*/```|
|```imageToSVG```|```BufferedImage image, HashMap<String,Float> options /*can be null*/, byte [][] palette /*can be null*/```|```String /*SVG content*/```|
|```imagedataToSVG```|```ImageData imgd, HashMap<String,Float> options /*can be null*/, byte [][] palette /*can be null*/```|```String /*SVG content*/```|
|```imageToTracedata```|```String filename, HashMap<String,Float> options /*can be null*/, byte [][] palette /*can be null*/```|```IndexedImage /*read the source for details*/```|
|```imageToTracedata```|```BufferedImage image, HashMap<String,Float> options /*can be null*/, byte [][] palette /*can be null*/```|```IndexedImage /*read the source for details*/```|
|```imagedataToTracedata```|```ImageData imgd, HashMap<String,Float> options /*can be null*/, byte [][] palette /*can be null*/```|```IndexedImage /*read the source for details*/```|


#### Helper Functions
|Function name|Arguments|Returns|
|-------------|---------|-------|
|```saveString```|```String filename, String str```|```void```|
|```loadImageData```|```String filename```|```ImageData /*read the source for details*/```|
|```loadImageData```|```BufferedImage image```|```ImageData /*read the source for details*/```|

```ImageData``` is similar to [ImageData](https://developer.mozilla.org/en-US/docs/Web/API/ImageData) here.

There are more functions for advanced users, read the source if you are interested. :)

### Options<br>
### 配置选项<br>
|Option name|Default value|Meaning|
|-----------|-------------|-------|
|```ltres```|```1f```|Error treshold for straight lines.|
|```qtres```|```1f```|Error treshold for quadratic splines.|
|```pathomit```|```8f```|Edge node paths shorter than this will be discarded for noise reduction.|
|```colorsampling```|```1f```|Enable or disable color sampling. 1f is on, 0f is off.|
|```numberofcolors```|```16f```|Number of colors to use on palette if pal object is not defined.|
|```mincolorratio```|```0.02f```|Color quantization will randomize a color if fewer pixels than (total pixels*mincolorratio) has it.|
|```colorquantcycles```|```3f```|Color quantization will be repeated this many times.|
|```blurradius```|```0f```|Set this to 1f..5f for selective Gaussian blur preprocessing.|
|```blurdelta```|```20f```|RGBA delta treshold for selective Gaussian blur preprocessing.|
|```scale```|```1f```|Every coordinate will be multiplied with this, to scale the SVG.|
|```roundcoords```|```1f```|rounding coordinates to a given decimal place. 1f means rounded to 1 decimal place like 7.3 ; 3f means rounded to 3 places, like 7.356|
|```viewbox```|```0f```|Enable or disable SVG viewBox. 1f is on, 0f is off.|
|```desc```|```1f```|Enable or disable SVG descriptions. 1f is on, 0f is off.|
|```lcpr```|```0f```|Straight line control point radius, if this is greater than zero, small circles will be drawn in the SVG. Do not use this for big/complex images.|
|```qcpr```|```0f```|Quadratic spline control point radius, if this is greater than zero, small circles and lines will be drawn in the SVG. Do not use this for big/complex images.|

### Process overview
See [Process overview and Ideas for improvement](https://github.com/jankovicsandras/imagetracerjava/blob/master/process_overview.md)

### License<br>
### 许可证<br>
Modfiy Author：田梓萱<br>
ImageTracerJava 在 [Apache 2.0 License](LICENSE)下获得许可