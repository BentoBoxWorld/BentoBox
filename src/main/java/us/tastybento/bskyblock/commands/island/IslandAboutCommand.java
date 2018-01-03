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
    }
    
    @Override
    public void setup() {
        this.setDescription("commands.island.about.description");
    }

    
    @Override
    public boolean execute(User user, List<String> args) {
        user.sendRawMessage("About " + BSkyBlock.getInstance().getDescription().getName() + " v" + BSkyBlock.getInstance().getDescription().getVersion() + ":");
        user.sendRawMessage("Copyright (c) 2017 - 2018 tastybento, Poslovitch");
        user.sendRawMessage("All rights reserved.");
        user.sendRawMessage("");
        user.sendRawMessage("Redistribution and use in source and binary forms, with or without");
        user.sendRawMessage("modification, are permitted provided that the following conditions are met:");

        user.sendRawMessage("    * Redistributions of source code must retain the above copyright notice,");
        user.sendRawMessage("      this list of conditions and the following disclaimer.");

        user.sendRawMessage("    * Redistributions in binary form must reproduce the above copyright");
        user.sendRawMessage("      notice, this list of conditions and the following disclaimer in the");
        user.sendRawMessage("      documentation and/or other materials provided with the distribution.");

        user.sendRawMessage("    * Neither the name of the BSkyBlock team nor the names of its");
        user.sendRawMessage("      contributors may be used to endorse or promote products derived from");
        user.sendRawMessage("      this software without specific prior written permission.");

        user.sendRawMessage("THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"");
        user.sendRawMessage("AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE");
        user.sendRawMessage("IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE");
        user.sendRawMessage("ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE");
        user.sendRawMessage("LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR");
        user.sendRawMessage("CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF");
        user.sendRawMessage("SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS");
        user.sendRawMessage("INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN");
        user.sendRawMessage("CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)");
        user.sendRawMessage("ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE");
        user.sendRawMessage("POSSIBILITY OF SUCH DAMAGE. ");
        return false;
    }

}
