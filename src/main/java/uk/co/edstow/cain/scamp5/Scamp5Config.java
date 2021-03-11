package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.scamp5.output.Scamp5OutputFormatter;

public abstract class Scamp5Config<SELF extends Scamp5Config<SELF>> {
    public final Scamp5OutputFormatter outputFormatter;

    public Scamp5Config(Scamp5OutputFormatter outputFormatter) {
        this.outputFormatter = outputFormatter;
    }

    public abstract Scamp5ConfigBuilder<SELF> builder();

    public static abstract class Scamp5ConfigBuilder<C extends Scamp5Config<C>> {
        protected Scamp5OutputFormatter outputFormatter;
        public Scamp5ConfigBuilder<C> setOutputFormatter(Scamp5OutputFormatter outputFormatter) {
            this.outputFormatter = outputFormatter;
            return this;
        }

        public abstract C build();
    }
}
