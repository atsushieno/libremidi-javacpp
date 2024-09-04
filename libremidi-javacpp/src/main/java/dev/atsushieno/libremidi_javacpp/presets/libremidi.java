package dev.atsushieno.libremidi_javacpp.presets;

import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.annotation.*;
import org.bytedeco.javacpp.tools.*;

@Properties(
    value = {
        @Platform(include = "<libremidi/libremidi-c.h>", link = "libremidi")
    },
    target = "dev.atsushieno.libremidi_javacpp",
    global = "dev.atsushieno.libremidi_javacpp.global.libremidi"
)
public class libremidi implements InfoMapper {
    static { Loader.checkVersion("dev.atsushieno", "libremidi_javacpp"); }

    public void map(InfoMap infoMap) {
        infoMap
                .put(new Info("LIBREMIDI_EXPORT").cppText("#define LIBREMIDI_EXPORT"))

                // FIXME: they should be enabled by fixing duplicate output constructs...
                // error: class Callback_Pointer_BytePointer_long_Pointer is already defined in class libremidi_midi_configuration
                .put(new Info("on_error").skip())
                .put(new Info("on_warning").skip())
                // error: class Callback_Pointer_libremidi_midi_in_port is already defined in class libremidi_observer_configuration
                .put(new Info("input_added").skip())
                .put(new Info("input_removed").skip())
                .put(new Info("output_added").skip())
                .put(new Info("output_removed").skip())
                // FIXME: https://github.com/atsushieno/libremidi-javacpp/issues/3
                //.put(new Info().cppTypes("libremidi_midi_configuration").cppText("int version"))
            ;
    }
}
