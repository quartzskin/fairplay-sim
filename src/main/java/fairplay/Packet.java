package fairplay;

record Packet(int player, long seq, long clientMs, double x, double y,
              int health, int ammo, int shots, int reloads) {}
