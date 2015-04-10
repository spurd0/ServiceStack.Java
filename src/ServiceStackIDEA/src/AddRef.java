import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.ide.util.PackageChooserDialog;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl;
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileImpl;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.impl.file.PsiDirectoryImpl;
import com.intellij.psi.util.ClassUtil;
import com.intellij.ui.JBColor;
import com.sun.javafx.fxml.builder.URLBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AddRef extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextPane errorTextPane;
    private JTextField addressUrlTextField;
    private TextFieldWithBrowseButton packageBrowse;
    private JTextField nameTextField;
    private JTextPane infoTextPane;
    private Module module;

    private String errorMessage;
    private PsiPackage selectedPackage;
    private String selectedDirectory;

    public AddRef(@NotNull Module module) {
        this.module = module;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        ImageIcon imageIcon = createImageIcon("/icons/logo-16.png", "ServiceStack");
        if (imageIcon != null) {
            this.setIconImage(imageIcon.getImage());
        }
        errorTextPane.setForeground(JBColor.RED);

        buttonOK.setEnabled(false);

        addressUrlTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = null;
                if (input instanceof JTextField) {
                    text = ((JTextField) input).getText();
                }

                return text != null && text.length() > 0;
            }

            @Override
            public boolean shouldYieldFocus(JComponent input) {
                boolean valid = verify(input);
                if (!valid) {
                    errorMessage = "URL Address is required";
                    errorTextPane.setVisible(true);
                    errorTextPane.setText(errorMessage);
                }

                return true;
            }
        });
        nameTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = null;
                if (input instanceof JTextField) {
                    text = ((JTextField) input).getText();
                }

                return text != null && text.length() > 0;
            }

            @Override
            public boolean shouldYieldFocus(JComponent input) {
                boolean valid = verify(input);
                if (!valid) {
                    errorMessage = "A file name is required.";
                    errorTextPane.setVisible(true);
                    errorTextPane.setText(errorMessage);
                }

                return true;
            }
        });

        nameTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                warn();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                warn();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                warn();
            }

            public void warn() {
                if (nameTextField.getInputVerifier().verify(nameTextField) && addressUrlTextField.getInputVerifier().verify(addressUrlTextField)) {
                    buttonOK.setEnabled(true);
                } else {
                    buttonOK.setEnabled(false);
                }
            }
        });

        addressUrlTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                warn();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                warn();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                warn();
            }

            public void warn() {
                if (nameTextField.getInputVerifier().verify(nameTextField) && addressUrlTextField.getInputVerifier().verify(addressUrlTextField)) {
                    buttonOK.setEnabled(true);
                } else {
                    buttonOK.setEnabled(false);
                }
            }
        });

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                processOK();
            }
        });

        packageBrowse.addActionListener(new BrowsePackageListener(packageBrowse, module.getProject(), "Browse packages"));

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    public void setSelectedPackage(@NotNull PsiPackage selectedPackage) {
        this.selectedPackage = selectedPackage;
        setPackageBrowseText(selectedPackage.getQualifiedName());
    }

    public void setSelectedDirectory(@NotNull String selectedDirectory) {
        this.selectedDirectory = selectedDirectory;
    }

    private void processOK() {
        buttonOK.setEnabled(false);
        buttonCancel.setEnabled(false);
        errorMessage = null;
        errorTextPane.setVisible(false);

        Runnable r = new Runnable() {
            public void run() {
                try {
                    onOK();
                } catch (Exception e1) {
                    e1.printStackTrace();
                    errorMessage = errorMessage != null ? errorMessage : "An error occurred adding reference - " + e1.getMessage();
                }
                if (errorMessage != null) {
                    errorTextPane.setVisible(true);
                    errorTextPane.setText(errorMessage);
                }
                buttonOK.setEnabled(true);
                buttonCancel.setEnabled(true);

            }
        };
        SwingUtilities.invokeLater(r);
    }

    private void setPackageBrowseText(String packageName) {
        packageBrowse.setText(packageName);
    }

    private ImageIcon createImageIcon(String path, String description) {
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    private void onOK() {
        String url;
        List<String> javaCodeLines = new ArrayList<>();
        try {
            URIBuilder urlBuilder = createUrl(addressUrlTextField.getText());
            urlBuilder.addParameter("Package", packageBrowse.getText());
            String name = getDtoNameWithoutExtention().replaceAll("\\.", "_");
            urlBuilder.addParameter("GlobalNamespace", name);
            url = urlBuilder.build().toString();

            URL serviceUrl = new URL(url);
            URLConnection javaResponseConnection = serviceUrl.openConnection();
            BufferedReader javaResponseReader = new BufferedReader(
                    new InputStreamReader(
                            javaResponseConnection.getInputStream()));
            String metadataInputLine;

            while ((metadataInputLine = javaResponseReader.readLine()) != null)
                javaCodeLines.add(metadataInputLine);

            javaResponseReader.close();

            if(!javaCodeLines.get(0).startsWith("/* Options:")) {
                //Invalid endpoint
                errorMessage = "The address url is not a valid ServiceStack endpoint.";
                return;
            }

        } catch (URISyntaxException | MalformedURLException e) {
            e.printStackTrace();
            errorMessage = e.getClass().getName() + " - Invalid ServiceStack endpoint provided - " + addressUrlTextField.getText();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            errorMessage = e.getClass().getName() + " - Failed to read response - " + addressUrlTextField.getText();
            return;
        }


        GradleBuildFileHelper gradleBuildFileHelper = new GradleBuildFileHelper(this.module);
        boolean showDto = false;
        if(gradleBuildFileHelper.addDependency("net.servicestack", "android", "0.0.1")) {
            refreshBuildFile();
        } else {
            showDto = true;
        }

        String dtoPath;
        try {
            dtoPath = getDtoPath();
        } catch (Exception e) {
            return;
        }

        if (!writeDtoFile(javaCodeLines, dtoPath)) {
            return;
        }
        refreshFile(dtoPath, showDto);
        VirtualFileManager.getInstance().syncRefresh();
        dispose();
    }

    private boolean writeDtoFile(List<String> javaCode, String path) {
        BufferedWriter writer = null;
        boolean result = true;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(path), "utf-8"));
            for (String item : javaCode) {
                writer.write(item);
                writer.newLine();
            }
        } catch (IOException ex) {
            result = false;
            errorMessage = "Error writing DTOs to file - " + ex.getMessage();
        } finally {
            try {
                assert writer != null;
                writer.close();
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    private String getDtoPath() throws FileNotFoundException {
        VirtualFile moduleFile = module.getModuleFile();
        if(moduleFile == null) {
            throw new FileNotFoundException("Module file not found. Unable to add DTO to project.");
        }
        String fullDtoPath;

        PsiPackage mainPackage = JavaPsiFacade.getInstance(module.getProject()).findPackage(packageBrowse.getText());
        if(mainPackage != null && mainPackage.isValid() && mainPackage.getDirectories().length > 0) {
            File foo = new File(selectedDirectory);
            VirtualFile selectedFolder = LocalFileSystem.getInstance().findFileByIoFile(foo);
            if(selectedFolder == null) {
                errorMessage = "Unable to determine path for DTO file.";
                throw new FileNotFoundException();
            }
            PsiDirectory rootPackageDir = PsiManager.getInstance(module.getProject()).findDirectory(selectedFolder);
            fullDtoPath = rootPackageDir.getVirtualFile().getPath() + "/" + getDtoFileName();
        } else {
            String moduleSourcePath;
            if(moduleFile.getParent() == null) {
                moduleSourcePath = moduleFile.getPath() + "/main/java";
            } else {
                moduleSourcePath = moduleFile.getParent().getPath() + "/src/main/java";
            }
            fullDtoPath = moduleSourcePath + "/" + getDtoFileName();
        }
        return fullDtoPath;
    }

    private void refreshBuildFile() {
        VirtualFileManager.getInstance().syncRefresh();
        VirtualFile fileByUrl = VirtualFileManager.getInstance().findFileByUrl(module.getModuleFile().getParent().getUrl() + "/build.gradle");

        FileEditorManager.getInstance(module.getProject()).openFile(fileByUrl, false);
        Document document = FileEditorManager.getInstance(module.getProject()).getSelectedTextEditor().getDocument();

        FileDocumentManager.getInstance().reloadFromDisk(document);
        VirtualFileManager.getInstance().syncRefresh();
    }

    private void refreshFile(String filePath, boolean openFile) {
        VirtualFileManager.getInstance().syncRefresh();
        File file = new File(filePath);
        VirtualFile fileByUrl = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);

        if (fileByUrl == null) {
            return;
        }

        FileEditorManager.getInstance(module.getProject()).openFile(fileByUrl, false);
        Document document = FileEditorManager.getInstance(module.getProject()).getSelectedTextEditor().getDocument();

        if (!openFile) FileEditorManager.getInstance(module.getProject()).closeFile(fileByUrl);

        FileDocumentManager.getInstance().reloadFromDisk(document);
        VirtualFileManager.getInstance().syncRefresh();
    }

    private class BrowsePackageListener implements ActionListener {
        private TextFieldWithBrowseButton _textField;
        private Project _project;
        private String _title;

        public BrowsePackageListener(TextFieldWithBrowseButton textField, Project project, String title) {
            _textField = textField;
            _project = project;
            _title = title;
        }

        public void actionPerformed(ActionEvent e) {
            PackageChooserDialog dialog = new PackageChooserDialog(_title, _project);
            dialog.selectPackage(_textField.getText());
            dialog.show();

            if (dialog.getExitCode() == PackageChooserDialog.CANCEL_EXIT_CODE) {
                return;
            }
            selectedPackage = dialog.getSelectedPackage();
            _textField.setText(dialog.getSelectedPackage().getQualifiedName());
        }
    }

    private URIBuilder createUrl(String text) throws MalformedURLException, URISyntaxException {
        String serverUrl = text.endsWith("/") ? text : (text + "/");
        serverUrl = (serverUrl.startsWith("http://") || serverUrl.startsWith("https://")) ? serverUrl : ("http://" + serverUrl);
        URL url = new URL(serverUrl);
        String path = url.getPath().contains("?") ? url.getPath().split("\\?", 2)[0] : url.getPath();
        if (!path.endsWith("types/java/")) {
            serverUrl += "types/java/";
        }
        URIBuilder builder;

        try {
            builder = new URIBuilder(serverUrl);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw e;
        }


        return builder;
    }

    private String getDtoFileName() {
        String name = nameTextField.getText();
        int p = name.lastIndexOf(".");
        String e = name.substring(p + 1);
        if (p == -1 || !Objects.equals(e, "java")) {
            /* file has no extension */
            return name + ".java";
        } else {
            /* file has extension e */
            return name;
        }
    }

    private String getDtoNameWithoutExtention() {
        String name = nameTextField.getText();
        int p = name.lastIndexOf(".");
        String e = name.substring(p + 1);
        if (p == -1 || !Objects.equals(e, "java")) {
            /* file has no extension */
            return name;
        } else {
            /* file has extension e */
            return name.substring(0, p);
        }
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
        AddRef dialog = new AddRef(null);
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
