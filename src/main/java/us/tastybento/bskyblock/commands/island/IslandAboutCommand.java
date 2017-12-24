package us.tastybento.bskyblock.commands.island;

import java.util.List;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;

public class IslandAboutCommand extends CompositeCommand {

    /**
     * About
     * @param islandCommand
     */
    public IslandAboutCommand(CompositeCommand islandCommand) {
        super(islandCommand, "about", "ab");
        this.setUsage("commands.island.about.usage");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        user.sendLegacyMessage("About " + BSkyBlock.getPlugin().getDescription().getName() + " v" + BSkyBlock.getPlugin().getDescription().getVersion() + ":");
        user.sendLegacyMessage("Copyright (c) 2017 - 2018 tastybento, Poslovitch");
        user.sendLegacyMessage("All rights reserved.");
        user.sendLegacyMessage("");
        user.sendLegacyMessage("Redistribution and use in source and binary forms, with or without");
        user.sendLegacyMessage("modification, are permitted provided that the following conditions are met:");

        user.sendLegacyMessage("    * Redistributions of source code must retain the above copyright notice,");
        user.sendLegacyMessage("      this list of conditions and the following disclaimer.");

        user.sendLegacyMessage("    * Redistributions in binary form must reproduce the above copyright");
        user.sendLegacyMessage("      notice, this list of conditions and the following disclaimer in the");
        user.sendLegacyMessage("      documentation and/or other materials provided with the distribution.");

        user.sendLegacyMessage("    * Neither the name of the BSkyBlock team nor the names of its");
        user.sendLegacyMessage("      contributors may be used to endorse or promote products derived from");
        user.sendLegacyMessage("      this software without specific prior written permission.");

        user.sendLegacyMessage("THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"");
        user.sendLegacyMessage("AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE");
        user.sendLegacyMessage("IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE");
        user.sendLegacyMessage("ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE");
        user.sendLegacyMessage("LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR");
        user.sendLegacyMessage("CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF");
        user.sendLegacyMessage("SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS");
        user.sendLegacyMessage("INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN");
        user.sendLegacyMessage("CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)");
        user.sendLegacyMessage("ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE");
        user.sendLegacyMessage("POSSIBILITY OF SUCH DAMAGE. ");
        return false;
    }

}
