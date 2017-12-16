package us.tastybento.bskyblock.commands.island;

import java.util.Set;

import org.bukkit.command.CommandSender;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.CommandArgument;

public class IslandAboutCommand extends CommandArgument {

    public IslandAboutCommand() {
        super("about", "ab");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage("About " + BSkyBlock.getPlugin().getDescription().getName() + " v" + BSkyBlock.getPlugin().getDescription().getVersion() + ":");
        sender.sendMessage("Copyright (c) 2017 - 2018 tastybento, Poslovitch");
        sender.sendMessage("All rights reserved.");
        sender.sendMessage("");
        sender.sendMessage("Redistribution and use in source and binary forms, with or without");
        sender.sendMessage("modification, are permitted provided that the following conditions are met:");

        sender.sendMessage("    * Redistributions of source code must retain the above copyright notice,");
        sender.sendMessage("      this list of conditions and the following disclaimer.");

        sender.sendMessage("    * Redistributions in binary form must reproduce the above copyright");
        sender.sendMessage("      notice, this list of conditions and the following disclaimer in the");
        sender.sendMessage("      documentation and/or other materials provided with the distribution.");

        sender.sendMessage("    * Neither the name of the BSkyBlock team nor the names of its");
        sender.sendMessage("      contributors may be used to endorse or promote products derived from");
        sender.sendMessage("      this software without specific prior written permission.");

        sender.sendMessage("THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"");
        sender.sendMessage("AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE");
        sender.sendMessage("IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE");
        sender.sendMessage("ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE");
        sender.sendMessage("LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR");
        sender.sendMessage("CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF");
        sender.sendMessage("SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS");
        sender.sendMessage("INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN");
        sender.sendMessage("CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)");
        sender.sendMessage("ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE");
        sender.sendMessage("POSSIBILITY OF SUCH DAMAGE. ");
        return false;
    }

    @Override
    public Set<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
