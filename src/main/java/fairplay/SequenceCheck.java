package fairplay;

import java.util.Optional;

final class SequenceCheck extends Check {
    SequenceCheck(Params params) {
        super("seq", params, 1);
    }

    @Override
    Optional<String> inspect(Context c) {
        return switch (c.order()) {
            case DUPLICATE -> Optional.of("seq " + c.packet().seq() + " seen already");
            case STALE -> Optional.of("seq " + c.packet().seq() + " outside reorder window");
            default -> Optional.empty();
        };
    }
}
