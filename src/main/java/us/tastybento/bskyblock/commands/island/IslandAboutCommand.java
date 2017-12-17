package us.tastybento.bskyblock.commands.island;

import java.util.Set;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.CommandArgument;
import us.tastybento.bskyblock.api.commands.User;

public class IslandAboutCommand extends CommandArgument {

    public IslandAboutCommand() {
        super("about", "ab");
    }

    @Override
    public boolean execute(User user, String[] args) {
        user.sendMessage("About " + BSkyBlock.getPlugin().getDescription().getName() + " v" + BSkyBlock.getPlugin().getDescription().getVersion() + ":");
        user.sendMessage("Copyright (c) 2017 - 2018 tastybento, Poslovitch");
        user.sendMessage("All rights reserved.");
        user.sendMessage("");
        user.sendMessage("Redistribution and use in source and binary forms, with or without");
        user.sendMessage("modification, are permitted provided that the following conditions are met:");

        user.sendMessage("    * Redistributions of source code must retain the above copyright notice,");
        user.sendMessage("      this list of conditions and the following disclaimer.");

        user.sendMessage("    * Redistributions in binary form must reproduce the above copyright");
        user.sendMessage("      notice, this list of conditions and the following disclaimer in the");
        user.sendMessage("      documentation and/or other materials provided with the distribution.");

        user.sendMessage("    * Neither the name of the BSkyBlock team nor the names of its");
        user.sendMessage("      contributors may be used to endorse or promote products derived from");
        user.sendMessage("      this software without specific prior written permission.");

        user.sendMessage("THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"");
        user.sendMessage("AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE");
        user.sendMessage("IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE");
        user.sendMessage("ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE");
        user.sendMessage("LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR");
        user.sendMessage("CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF");
        user.sendMessage("SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS");
        user.sendMessage("INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN");
        user.sendMessage("CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)");
        user.sendMessage("ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE");
        user.sendMessage("POSSIBILITY OF SUCH DAMAGE. ");
        return false;
    }

    @Override
    public Set<String> tabComplete(User sender, String[] args) {
        return null;
    }
}
