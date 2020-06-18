package roo.display.imageimporter;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;
import roo.display.imageimporter.ImportOptions.*;

public class ImageImporter extends JFrame {
  private JLabel imageLabel;
  private BufferedImage image;
  private Set<Integer> colorPalette = new HashSet<>();
  Encoding encoding;
  private JMenuItem sMenuItem;
  private File inputFile;
  private File currentDirectory;
  private Compression compression;
  private Storage storage;

  public static void main(String[] args) throws Throwable {
    try {
      CommandLine.call(new Main(), args);
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }

  @Command(description = "Imports specified images to be used with the roo.display library", name = "imageimporter", mixinStandardHelpOptions = true, version = "1.0")
  private static class Main implements Callable<Void> {
    @Option(names = { "-e", "--encoding" }, description = "color encoding")
    Encoding encoding = Encoding.ARGB8888;

    @Option(names = { "-c", "--compression" }, description = "compression type")
    Compression compression = Compression.NONE;

    @Option(names = { "-s", "--storage" }, description = "where to store image data")
    Storage storage = Storage.PROGMEM;

    @Option(names = { "--fg" }, description = "foreground color for monochrome and Alpha-only data")
    String fgColor;

    @Option(names = { "--bg" }, description = "background color for monochrome data")
    String bgColor;

    @Option(names = { "--output-dir" }, description = "where to place resulting image files. Defaults to cwd.")
    File outputDir;

    @Option(names = { "-o", "--output-name" },
            description = "if set, all images will be generated in a single file by that name. " +
                          "Otherwise, each image goes to a separate file.")
    String outputName;

    @Option(names = { "--input-dir" }, description = "Where to look for input files. Defaults to cwd.")
    File inputDir;

    @Option(names = { "--output-header-dir" },
            description = "where to place resulting header files. Defaults to output-dir")
    File outputHeaderDir;

    @Option(names = { "--output-payload-dir" },
            description = "where to place resulting SPIFFS data files. Defaults to output-dir")
    File outputPayloadDir;

    @Parameters(arity = "0..*", paramLabel = "FILE", description = "File(s) to process.")
    private File[] inputFiles;

    @Override
    public Void call() throws Exception {
      if (inputFiles == null || inputFiles.length == 0) {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        ImageImporter importer = new ImageImporter(encoding, compression, storage, outputDir);
        importer.init();
        importer.setVisible(true);
      } else {
        ImportOptions options = (new ImportOptions()).setEncoding(encoding).setStorage(storage)
            .setComporession(compression).setOutputDirectory(outputDir);
        if (bgColor != null) {
          options.setBgColor(bgColor);
        }
        if (fgColor != null) {
          options.setFgColor(fgColor);
        }
        if (outputHeaderDir != null) {
          options.setOutputHeaderDirectory(outputHeaderDir);
        }
        if (outputPayloadDir != null) {
          options.setOutputPayloadDirectory(outputPayloadDir);
        }

        if (outputName == null) {
          // Each image goes to a separate file.
          for (File input : inputFiles) {
            File absoluteInput = openFile(inputDir, input);
            String name = ImportOptions.getRecommendedNameFromInputFilename(absoluteInput.getName());
            BufferedImage image = ImageIO.read(absoluteInput);
            Core core = new Core(options, name);
            core.write(name, image);
            core.close();
          }
        } else {
          // All images go to a single file.
          Core core = new Core(options, outputName);
          for (File input : inputFiles) {
            File absoluteInput = openFile(inputDir, input);
            String name = ImportOptions.getRecommendedNameFromInputFilename(absoluteInput.getName());
            BufferedImage image = ImageIO.read(absoluteInput);
            core.write(name, image);
          }
          core.close();
        }
      }
      return null;
    }
  }

  // Helper.
  private static File openFile(File inputDir, File input) throws IOException {
    File absoluteInput = input.isAbsolute() ? input : new File(inputDir, input.getPath());
    Logger.getGlobal().info("Loading file: " + absoluteInput.getAbsolutePath());
    if (!absoluteInput.exists()) {
      throw new FileNotFoundException(absoluteInput.getAbsolutePath());
    }
    if (!absoluteInput.canRead()) {
      throw new IOException("Cannot read " + absoluteInput.getAbsolutePath());
    }
    return absoluteInput;
  }

  public ImageImporter(Encoding encoding, Compression compression, Storage storage, File currentDirectory) {
    super("roo_display Image Importer");
    this.encoding = encoding;
    this.compression = compression;
    this.storage = storage;
    if (currentDirectory != null) {
      this.currentDirectory = currentDirectory;
    } else {
      this.currentDirectory = null;
    }
    init();
  }

  private void init() {
    setMinimumSize(new Dimension(320, 200));
    setLocationRelativeTo(null);
    createMenuBar();
    Container pane = getContentPane();
    pane.setLayout(new BorderLayout());

    imageLabel = new JLabel();
    pane.add(imageLabel, BorderLayout.CENTER);

    pack();
    setDefaultCloseOperation(EXIT_ON_CLOSE);
  }

  private class EncodingOption extends AbstractAction {
    private Encoding encoding;

    public EncodingOption(Encoding encoding) {
      super(encoding.description);
      this.encoding = encoding;
      putValue(AbstractAction.SELECTED_KEY, encoding == ImageImporter.this.encoding);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      ImageImporter.this.encoding = encoding;
    }
  }

  private void createMenuBar() {
    JMenuBar menubar = new JMenuBar();
    JMenu file = new JMenu("File");
    file.setMnemonic(KeyEvent.VK_F);

    JMenuItem oMenuItem = new JMenuItem("Open");
    oMenuItem.setMnemonic(KeyEvent.VK_O);
    oMenuItem.setToolTipText("Open image file");
    oMenuItem.addActionListener((ActionEvent event) -> {
      openImage();
    });
    file.add(oMenuItem);

    sMenuItem = new JMenuItem("Save");
    sMenuItem.setMnemonic(KeyEvent.VK_S);
    sMenuItem.setToolTipText("Save image file for roo_display");
    sMenuItem.addActionListener((ActionEvent event) -> {
      saveImage();
    });
    sMenuItem.setEnabled(false);
    // sMenuItem.setModel(new DefaultButtonModel() {
    // public boolean isEnabled() { return ImageImporter.this.image != null; }
    // });
    file.add(sMenuItem);
    file.addSeparator();

    JMenuItem eMenuItem = new JMenuItem("Exit");
    eMenuItem.setMnemonic(KeyEvent.VK_E);
    eMenuItem.setToolTipText("Exit application");
    eMenuItem.addActionListener((ActionEvent event) -> {
      System.exit(0);
    });
    file.add(eMenuItem);

    menubar.add(file);
    setJMenuBar(menubar);

    JMenu format = new JMenu("Format");
    file.setMnemonic(KeyEvent.VK_R);

    ButtonGroup group = new ButtonGroup();
    group.add(new JRadioButtonMenuItem(new EncodingOption(Encoding.ARGB8888)));

    group.add(new JRadioButtonMenuItem(new EncodingOption(Encoding.ARGB6666)));

    group.add(new JRadioButtonMenuItem(new EncodingOption(Encoding.ARGB4444)));

    group.add(new JRadioButtonMenuItem(new EncodingOption(Encoding.RGB565)));

    // group.add(new JRadioButtonMenuItem(
    // new EncodingOption(Encoding.RGB565_ALPHA4)));

    group.add(new JRadioButtonMenuItem(new EncodingOption(Encoding.GRAYSCALE8)));

    group.add(new JRadioButtonMenuItem(new EncodingOption(Encoding.GRAYSCALE4)));

    group.add(new JRadioButtonMenuItem(new EncodingOption(Encoding.ALPHA8)));

    group.add(new JRadioButtonMenuItem(new EncodingOption(Encoding.ALPHA4)));

    group.add(new JRadioButtonMenuItem(new EncodingOption(Encoding.MONOCHROME)));

    for (Enumeration<AbstractButton> options = group.getElements(); options.hasMoreElements();) {
      format.add(options.nextElement());
    }

    format.addSeparator();

    JMenuItem rle = new JCheckBoxMenuItem("RLE encoding");
    rle.addActionListener((ActionEventEvent) -> {
      compression = rle.isSelected() ? Compression.RLE : Compression.NONE;
    });
    rle.setSelected(compression == Compression.RLE);
    format.add(rle);

    JMenuItem spiffs = new JCheckBoxMenuItem("SPIFFS storage");
    spiffs.addActionListener((ActionEventEvent) -> {
      storage = spiffs.isSelected() ? Storage.SPIFFS : Storage.PROGMEM;
    });
    spiffs.setSelected(storage == Storage.SPIFFS);
    format.add(spiffs);

    menubar.add(format);
  }

  private void openImage() {
    FileFilter imageFilter = new FileNameExtensionFilter("Image files", ImageIO.getReaderFileSuffixes());
    JFileChooser fc = new JFileChooser();
    fc.addChoosableFileFilter(imageFilter);
    fc.setAcceptAllFileFilterUsed(false);
    int returnVal = fc.showDialog(this, "Open image");
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      inputFile = file;
      if (currentDirectory == null) {
        currentDirectory = file.getParentFile();
      }
      try {
        image = ImageIO.read(file);
        imageLabel.setIcon(new ImageIcon(image));
      } catch (IOException e) {
        Logger.getGlobal().severe(e.getMessage());
      }
    }
    colorPalette.clear();
    for (int i = 0; i < image.getHeight(); ++i) {
      for (int j = 0; j < image.getWidth(); ++j) {
        int rgb = image.getRGB(j, i);
        colorPalette.add(rgb);
      }
    }
    sMenuItem.setEnabled(true);
    pack();
  }

  private void saveImage() {
    JFileChooser fc = new JFileChooser(currentDirectory);
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fc.setSelectedFile(currentDirectory);
    int returnVal = fc.showSaveDialog(this);
    if (returnVal != JFileChooser.APPROVE_OPTION) {
      return;
    }

    ImportOptions options = new ImportOptions().initFromInput(inputFile).setStorage(storage).setEncoding(encoding)
        .setComporession(compression);
    String name = ImportOptions.getRecommendedNameFromInputFilename(inputFile.getName());

    try {
      Core core = new Core(options, name);
      core.write(name, image);
      core.close();
    } catch (IOException e) {
      Logger.getGlobal().severe(e.getMessage());
    }
  }

  // private void saveImageIn(String directory) throws IOException {
  //   JFileChooser fc = new JFileChooser(currentDirectory);
  //   fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
  //   fc.setSelectedFile(currentDirectory);
  //   int returnVal = fc.showSaveDialog(this);
  //   if (returnVal != JFileChooser.APPROVE_OPTION) {
  //     return;
  //   }

  //   ImportOptions options = new ImportOptions().initFromInput(inputFile).setStorage(storage).setEncoding(encoding)
  //       .setComporession(compression);
  //   String name = ImportOptions.getRecommendedNameFromInputFilename(inputFile.getName());

  //   try {
  //     Core.FileWriter w = new Core.FileWriter(options, "");
  //     w.write(name, image);
  //     w.close();
  //   } catch (IOException e) {
  //     Logger.getGlobal().severe(e.getMessage());
  //   }
  // }
}
