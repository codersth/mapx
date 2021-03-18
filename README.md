# Foobar

A lib as an attachment for third map sdk, developers can extend its function depend on it.

## Usage

1、Add it in your root build.gradle at the end of repositories:
```groovy
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
2、Add dependency.
```groovy
implementation 'com.github.codersth:mapx:1.0-alpha'
```

3、Extends your own functions, you can see samples in project.

## Examples
1、Cluster markers with district.
![Alt text](screenshot/device-2021-03-18-103631.png?raw=true "Title")
