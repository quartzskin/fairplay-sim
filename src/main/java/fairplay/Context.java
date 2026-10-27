package fairplay;

record Context(PlayerState state, Packet packet, long arrivalMs, Order order) {}
