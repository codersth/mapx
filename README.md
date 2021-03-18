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
