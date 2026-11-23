package dev.octoclient.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

public class McRealScreen extends Screen {
    private static final int ROW_HEIGHT = 18;
    private static final int MAX_ROWS = 16;
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final Path folder;
    private record ScreenshotEntry(Path path, long modifiedAt) {}

    private final List<ScreenshotEntry> screenshots;
    private final String loadError;
    private int scroll;
    private String status = "Click a screenshot to open it";

    public McRealScreen(Path folder) {
        super(Component.literal("McReal Local Gallery"));
        this.folder = folder;
        List<ScreenshotEntry> files = List.of();
        String error = "";
        try {
            if (Files.isDirectory(folder)) {
                try (var stream = Files.list(folder)) {
                    List<ScreenshotEntry> loaded = new ArrayList<>();
                    for (Path path : stream.filter(Files::isRegularFile).filter(McRealScreen::isImage).toList()) {
                        try {
                            loaded.add(new ScreenshotEntry(path, Files.getLastModifiedTime(path).toMillis()));
                        } catch (IOException fileError) {
                            System.err.println("[OctoClient] Could not read screenshot date for " + path + ": "
                                    + fileError.getMessage());
                        }
                    }
                    files = loaded.stream()
                            .sorted(Comparator.comparingLong(ScreenshotEntry::modifiedAt).reversed())
                            .limit(200)
                            .toList();
                }
            }
        } catch (IOException ioError) {
            error = "Could not read screenshot folder: " + ioError.getMessage();
            System.err.println("[OctoClient] " + error);
        }
        screenshots = files;
        loadError = error;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0xF0101010);
        graphics.centeredText(font, "McReal - Local Screenshot Gallery", width / 2, 18, 0xFFFFC857);
        graphics.centeredText(font, folder.toString(), width / 2, 34, 0xFFB8BBC8);
        graphics.centeredText(font, "Up/Down to scroll; click an entry to open it; Esc to close",
                width / 2, 50, 0xFFB8BBC8);

        if (!loadError.isEmpty()) {
            graphics.centeredText(font, loadError, width / 2, 80, 0xFFFF5F6D);
        } else if (screenshots.isEmpty()) {
            graphics.centeredText(font, "No screenshots found in this folder", width / 2, 80, 0xFFFFFFFF);
        } else {
            int rows = Math.min(MAX_ROWS, screenshots.size() - scroll);
            int startY = 68;
            for (int i = 0; i < rows; i++) {
                int index = scroll + i;
                ScreenshotEntry entry = screenshots.get(index);
                Path path = entry.path();
                int y = startY + i * ROW_HEIGHT;
                if (mouseX >= 28 && mouseX <= width - 28 && mouseY >= y && mouseY < y + ROW_HEIGHT) {
                    graphics.fill(28, y, width - 28, y + ROW_HEIGHT, 0x664A5070);
                }
                String row = path.getFileName() + "    " + DATE_FORMAT.format(Instant.ofEpochMilli(entry.modifiedAt()));
                graphics.text(font, row, 38, y + 4, 0xFFFFFFFF, true);
            }
        }
        graphics.centeredText(font, status, width / 2, height - 24, 0xFFB8BBC8);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || screenshots.isEmpty()) {
            return super.mouseClicked(event, doubleClick);
        }
        int row = ((int) event.y() - 68) / ROW_HEIGHT + scroll;
        int visibleRows = Math.min(MAX_ROWS, screenshots.size() - scroll);
        if (event.x() < 28 || event.x() > width - 28 || event.y() < 68
                || row < scroll || row >= scroll + visibleRows || row >= screenshots.size()) {
            return super.mouseClicked(event, doubleClick);
        }
        openScreenshot(screenshots.get(row).path());
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_DOWN) {
            scroll = Math.min(Math.max(0, screenshots.size() - MAX_ROWS), scroll + 1);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_UP) {
            scroll = Math.max(0, scroll - 1);
            return true;
        }
        return super.keyPressed(event);
    }

    private void openScreenshot(Path screenshot) {
        if (!Desktop.isDesktopSupported()) {
            status = "Opening images is not supported on this system";
            return;
        }
        try {
            Desktop.getDesktop().open(screenshot.toFile());
            status = "Opened " + screenshot.getFileName();
        } catch (IOException | UnsupportedOperationException error) {
            status = "Could not open screenshot: " + error.getMessage();
            System.err.println("[OctoClient] " + status);
        }
    }

    private static boolean isImage(Path path) {
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

}
