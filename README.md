# roo_display_image_importer
Utility to import static images for use with the [roo_display](https://github.com/dejwk/roo_display), to use on microcontrollers with graphical displays.

You can start the utility without parameters. It will then open an interactive UI.

You can also use the utility as a command-line tool. Some options are only available this way:

```
Usage: imageimporter [-hV] [--autocrop] [--bg=<bgColor>] [--fg=<fgColor>]
                     [--input-dir=<inputDir>] [--output-dir=<outputDir>]
                     [--output-header-dir=<outputHeaderDir>]
                     [--output-payload-dir=<outputPayloadDir>]
                     [-c=<compression>] [-e=<encoding>] [-o=<outputName>]
                     [-s=<storage>] [FILE...]
Imports specified images to be used with the roo.display library
      [FILE...]             File(s) to process.
      --autocrop            if true, crops content to visible.
      --bg=<bgColor>        background color for monochrome data
      --fg=<fgColor>        foreground color for monochrome and Alpha-only data
      --input-dir=<inputDir>
                            Where to look for input files. Defaults to cwd.
      --output-dir=<outputDir>
                            where to place resulting image files. Defaults to cwd.
      --output-header-dir=<outputHeaderDir>
                            where to place resulting header files. Defaults to
                              output-dir
      --output-payload-dir=<outputPayloadDir>
                            where to place resulting SPIFFS data files. Defaults to
                              output-dir
  -c, --compression=<compression>
                            compression type
  -e, --encoding=<encoding> color encoding
  -h, --help                Show this help message and exit.
  -o, --output-name=<outputName>
                            if set, all images will be generated in a single file by
                              that name. Otherwise, each image goes to a separate
                              file.
  -s, --storage=<storage>   where to store image data
  -V, --version             Print version information and exit.
```

The importer also supports SVG inputs in both the UI and CLI.

For CLI SVG imports, `--scale` multiplies the SVG's intrinsic rendered size before encoding. The option defaults to `1.0` and is ignored for raster inputs.

For Material Symbols from Google Fonts, use `bin/import-google-symbol`. It accepts a Google Fonts icon URL, downloads the matching SVG asset, and forwards it to the importer with pictogram-friendly defaults:

```sh
bin/import-google-symbol 'https://fonts.google.com/icons?icon.category=Home&selected=Material+Symbols+Outlined:arming_countdown:FILL@0;wght@0;GRAD@0;opsz@20&icon.size=20&icon.color=%23000000&icon.platform=web&icon.set=Material+Symbols'
```

The helper defaults to `ALPHA4` plus `RLE`, derives `--fg` from the Google Fonts URL, and uses `--scale` to match the requested icon size when Google only exposes a nearby SVG size bucket. Any extra arguments are forwarded to `bin/imageimporter`, so you can still set output directories, names, storage, or override the defaults.

### Supported encodings

* ARGB8888
* ARGB6666
* ARGB4444
* RGB565
* GRAYSCALE8
* GRAYSCALE4
* ALPHA8
* ALPHA4
* MONOCHROME
* INDEXED1
* INDEXED2
* INDEXED4
* INDEXED8

Note: before converting to indexed color modes, make sure that the image uses no more colors than allowed by the target palette size (2, 4, 16, and 256, for INDEXED1-4 respectively.) Otherwise, the least frequently used colors will be dropped.

### Supported compression options

* NONE (default)
* RLE

Note: for ALPHA4 & RLE, the dedicated 'biased' compression is used. For other color modes, the default RLE algorithm is used. 

### Supported storage modes

* PROGMEM (default)
* SPIFFS
