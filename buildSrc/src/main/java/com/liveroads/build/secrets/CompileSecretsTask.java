package com.liveroads.build.secrets;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.Base64;

public class CompileSecretsTask extends DefaultTask {

    @InputFile
    public File srcFile;
    @OutputFile
    public File destFile;
    @Input
    public String destPackageName;
    @Input
    public String constantsVisibility;

    @TaskAction
    public void compile() {
        final byte[] srcBytes = readSrcFile();
        final byte[] encryptionKey = generateRandomBytes(srcBytes.length);
        final byte[] srcBytesEncrypted = encrypt(srcBytes, encryptionKey);
        final String encryptionKeyBase64 = base64Encode(encryptionKey);
        final String srcBytesEncryptedBase64 = base64Encode(srcBytesEncrypted);
        final String destKotlinSourceCode = generateDestFile(srcBytes, srcBytesEncryptedBase64,
                encryptionKeyBase64);
        writeDestFile(destKotlinSourceCode);
    }

    @NotNull
    private byte[] readSrcFile() {
        final File file = srcFile;
        if (file == null) {
            throw new TaskFailedException("srcFile==null");
        }

        try {
            return readFile(file);
        } catch (final IOException e) {
            throw new TaskFailedException("reading srcFile failed: " + file.getAbsolutePath()
                    + ": " + e.getMessage());
        }
    }

    @NotNull
    private static byte[] readFile(@NotNull final File file) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final InputStream in;
        try {
            in = new FileInputStream(file);
        } catch (final IOException e) {
            throw new IOException("opening file for reading failed: " + e.getMessage());
        }

        try {
            final byte[] buf = new byte[1024];
            while (true) {
                final int readCount = in.read(buf);
                if (readCount < 0) {
                    break;
                }
                baos.write(buf, 0, readCount);
            }
        } finally {
            try {
                in.close();
            } catch (final IOException ignored) {
            }
        }

        return baos.toByteArray();
    }

    @NotNull
    private static byte[] generateRandomBytes(final int length) {
        final SecureRandom random = new SecureRandom();
        final byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    @NotNull
    private static byte[] encrypt(@NotNull final byte[] bytes,
            @NotNull final byte[] encryptionKey) {
        final byte[] cipherText = new byte[bytes.length];
        for (int i = bytes.length - 1; i >= 0; i--) {
            cipherText[i] = (byte) (bytes[i] + encryptionKey[i]);
        }
        return cipherText;
    }

    @NotNull
    private static String base64Encode(@NotNull final byte[] bytes) {
        final Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(bytes);
    }

    @NotNull
    private String generateDestFile(@NotNull final byte[] plainText, @NotNull final String cipherText,
            @NotNull final String encryptionKey) {
        final String packageName = destPackageName;
        if (packageName == null) {
            throw new TaskFailedException("destPackageName==null");
        }

        final String visibility = constantsVisibility;

        final CharArrayWriter caw = new CharArrayWriter();
        final PrintWriter out = new PrintWriter(caw);

        out.println("package " + packageName);
        out.println();
        out.println("/*");
        out.println();
        out.println("WARNING: DO NOT MAKE DIRECT CHANGES THIS FILE!");
        out.println();
        out.println("This file was generated from " + srcFile.getName());
        out.println("by the Gradle task " + getName());
        out.println("and will be overwritten when the task is run again.");
        out.println();
        out.println("The contents of " + srcFile.getName() + " that are encrypted in DATA are:");
        out.println();
        out.println(new String(plainText).trim());
        out.println();
        out.println("*/");
        out.println();
        if (visibility != null) out.print(visibility + " ");
        out.println("const val ENCRYPTION_KEY = \"" + encryptionKey + "\"");
        if (visibility != null) out.print(visibility + " ");
        out.println("const val DATA = \"" + cipherText + "\"");

        out.close();
        return new String(caw.toCharArray());
    }

    private void writeDestFile(@NotNull final String data) {
        final File file = destFile;
        if (file == null) {
            throw new TaskFailedException("destFile==null");
        }
        final File dir = file.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
            throw new TaskFailedException("unable to create destination directory: " + dir);
        }
        try {
            writeFile(file, data);
        } catch (final IOException e) {
            throw new TaskFailedException("writing to destFile failed: " + file.getAbsolutePath()
                    + ": " + e.getMessage());
        }
    }

    private static void writeFile(@NotNull final File file, @NotNull final String data) throws IOException {
        final byte[] dataUtf8 = data.getBytes("UTF-8");

        final FileOutputStream out;
        try {
            out = new FileOutputStream(file);
        } catch (final IOException e) {
            throw new IOException("opening file for writing failed: " + e.getMessage());
        }

        boolean closed = false;
        try {
            out.write(dataUtf8);
            out.close();
            closed = true;
        } finally {
            if (!closed) {
                try {
                    out.close();
                } catch (final IOException ignored) {
                }
            }
        }
    }

    public static final class TaskFailedException extends RuntimeException {

        public TaskFailedException(final String message) {
            super(message);
        }

    }

}
