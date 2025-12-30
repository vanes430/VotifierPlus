package com.vexsoftware.votifier.util;

import java.util.function.Consumer;

public class AsciiArt {

    // ANDA BISA MENGEDIT BAGIAN INI
    // Masukkan ASCII Art Anda baris demi baris di dalam tanda kutip.
    private static final String[] BANNER = {
            "                                                         ",
            "██╗   ██╗ ██████╗ ████████╗██╗███████╗██╗███████╗██████╗ ",
            "██║   ██║██╔═══██╗╚══██╔══╝██║██╔════╝██║██╔════╝██╔══██╗",
            "██║   ██║██║   ██║   ██║   ██║█████╗  ██║█████╗  ██████╔╝",
            "╚██╗ ██╔╝██║   ██║   ██║   ██║██╔══╝  ██║██╔══╝  ██╔══██╗",
            " ╚████╔╝ ╚██████╔╝   ██║   ██║██║     ██║███████╗██║  ██║",
            "  ╚═══╝   ╚═════╝    ╚═╝   ╚═╝╚═╝     ╚═╝╚══════╝╚═╝  ╚═╝",
            "                                                         ",
            "   > Fork maintained by vanes430                         ",
            "   > GitHub: https://github.com/vanes430/VotifierPlus    ",
            "                                                         " 
    };

    /**
     * Mencetak ASCII Art ke logger yang diberikan.
     * Gunakan method reference untuk logger.
     * Contoh Spigot/Bungee: AsciiArt.send(getLogger()::info);
     * Contoh Velocity/Sponge: AsciiArt.send(logger::info);
     *
     * @param loggerConsumer Consumer logger (misalnya logger::info)
     */
    public static void send(Consumer<String> loggerConsumer) {
        for (String line : BANNER) {
            loggerConsumer.accept(line);
        }
    }
}
