## uCrop'n'Edit - Image Cropping + Editing Library for Android

#### Using the source code of "<a href="https://github.com/Yalantis/uCrop">uCrop</a>", I’ve added four new features - ability to change Brightness, Contrast, Saturation and Sharpness.

<p align="center" style="background-color:#ededed">
  <img src="preview.gif" width="320" height="560">
</p>

## Usage

1. Include the library as a local library project.

   ```
   allprojects {
      repositories {
         mavenCentral()
      }
   }
   ```

   `implementation 'com.github.krokyze:ucropnedit:2.2.9'`

2. To use uCrop’n’Edit, you can follow the exact same methods as for uCrop: <a href="https://github.com/Yalantis/uCrop#usage">Usage</a>

If you have any interesting ideas for uCrop’n’Edit do not be afraid to let me know. You can easily leave a request and I will be happy to take a look at the viability of your proposal. I’ll be more than happy to try and add some new features to the library, if I think they can make it even better!

#### Please keep in mind that most of the work is developed by Yalantis, so if you find any bugs that’s not related to brightness, contrast, saturation or sharpness, you should add a new issue to their <a href="https://github.com/Yalantis/uCrop/issues">Github page</a> and as they will push update, I will pull it to make sure everything works as best as it can.

Massive thanks to guys from Yalantis for open sourcing this great library!

## Changelog (<a href="https://github.com/Yalantis/uCrop#changelog">uCrop Changelog</a>)

### Version 2.2.9 (based on uCrop 2.2.9)

### Version 2.2.8 (based on uCrop 2.2.8)

### Version 2.2.6-2 (based on uCrop 2.2.6)

- Brightness feature
- Contrast feature
- Saturation feature
- Sharpness feature (supported for devices with >= API 17)

## License

    Copyright 2017, Yalantis

    Software doesn't collect, store or transfer data to Yalantis or third parties.
    Emplacement of this Software is carried out locally at device.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
