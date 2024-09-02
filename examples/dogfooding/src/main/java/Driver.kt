
import dev.atsushieno.ktmidi.MidiPortDetails
import dev.atsushieno.ktmidi.MidiTransportProtocol
import dev.atsushieno.libremidi_javacpp.*
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.javacpp.SizeTPointer
import java.nio.ByteBuffer
import dev.atsushieno.libremidi_javacpp.global.libremidi as library

// copied from libremidi-javacpp
enum class DesktopPlatform {
    Linux,
    Windows,
    MacOS,
}
object API {
    val Unspecified = library.UNSPECIFIED
    val CoreMidi = library.COREMIDI
    val AlsaSeq = library.ALSA_SEQ
    val AlsaRaw = library.ALSA_RAW
    val JackMidi = library.JACK_MIDI
    val WindowsMM = library.WINDOWS_MM
    val WindowsUwp = library.WINDOWS_UWP
    val WebMidi = library.WEBMIDI // ktmidi-jvm-desktop wouldn't run on Web platform though
    val PipeWire = library.PIPEWIRE
    val AlsaSeqUmp = library.ALSA_SEQ_UMP
    val AlsaRawUmp = library.ALSA_RAW_UMP
    val CoreMidiUmp = library.COREMIDI_UMP
    val WindowsMidiServices = library.WINDOWS_MIDI_SERVICES
    val Dummy = library.DUMMY

    fun getPlatformDefault(platform: DesktopPlatform, transportProtocol: Int) =
        when(platform) {
            DesktopPlatform.Linux ->
                if (transportProtocol == 2) AlsaSeqUmp
                else AlsaSeq
            DesktopPlatform.Windows ->
                if (transportProtocol == 2) WindowsMidiServices
                else WindowsUwp
            DesktopPlatform.MacOS ->
                if (transportProtocol == 2) CoreMidiUmp
                else CoreMidi
        }
}
private fun guessPlatform(): DesktopPlatform {
    val os = System.getProperty("os.name")
    return when {
        os.startsWith("windows", true) -> DesktopPlatform.Windows
        os.startsWith("mac", true) -> DesktopPlatform.MacOS
        else -> DesktopPlatform.Linux
    }
}

private fun checkReturn(action: ()->Int) {
    val ret = action()
    if (ret != 0)
        throw Exception("Native error: $ret")
}

open class LibreMidiPortDetails(
    override val id: String,
    override val name: String
) : MidiPortDetails {
    override val manufacturer = "N/A" // N/A by libremidi
    override val version: String = "N/A" // N/A by libremidi
    override val midiTransportProtocol =
        /*if (access.supportsUmpTransport) MidiTransportProtocol.UMP
        else*/ MidiTransportProtocol.MIDI1
}

class LibreMidiRealInPortDetails(
    override val id: String,
    override val name: String,
    val port: libremidi_midi_in_port
) : LibreMidiPortDetails(id, name)

class LibreMidiRealOutPortDetails(
    override val id: String,
    override val name: String,
    val port: libremidi_midi_out_port
) : LibreMidiPortDetails( id, name)

fun listOutputs(observer: libremidi_midi_observer_handle): List<MidiPortDetails> {
    val list = mutableListOf<MidiPortDetails>()
    val outCB = object : Arg2_Pointer_libremidi_midi_out_port() {
        override fun call(ctx: Pointer?, port: libremidi_midi_out_port) {
            // FIXME: can we indeed limit the name to this size?
            val nameBuf = ByteArray(1024)
            val size = SizeTPointer(0)
            checkReturn { library.libremidi_midi_out_port_name(port, nameBuf, size) }
            val name = nameBuf.take(size.get().toInt()).toByteArray().decodeToString()
            println(name)
            list.add(LibreMidiRealOutPortDetails("Out_${list.size}", name, port))
        }
    }

    checkReturn { library.libremidi_midi_observer_enumerate_output_ports(observer, null, outCB) }
    return list
}

fun main(args: Array<String>) {

    val api = API.getPlatformDefault(guessPlatform(), 1)

    val obsConf = libremidi_observer_configuration()
    library.libremidi_midi_observer_configuration_init(obsConf)
    obsConf.track_virtual(true)
    obsConf.track_any(true)

    val apiConf = libremidi_api_configuration()
    library.libremidi_midi_api_configuration_init(apiConf)
    apiConf.api(api)

    val observer = libremidi_midi_observer_handle()
    library.libremidi_midi_observer_new(obsConf, apiConf, observer)
    /*
    listOutputs(observer).forEach {
        println("${it.id}: ${it.name}")
    }*/

    val useByteBuffer = false // This should not work
    val useBytePtr = true
    val cb = object : Arg2_Pointer_libremidi_midi_out_port() {
        override fun call(ctx: Pointer?, port: libremidi_midi_out_port) {
            val size = SizeTPointer(1024)
            lateinit var name: ByteArray
            if (useByteBuffer) {
                val nameBuf = ByteBuffer.allocateDirect(1024)
                println("libremidi_midi_out_port_name returned " + library.libremidi_midi_out_port_name(port, nameBuf, size))
                println("name size: " + size.get())
                name = ByteArray(size.get().toInt())
                nameBuf.get(name)
            } else if (useBytePtr) {
                val nameBuf = ByteBuffer.allocateDirect(1024)
                val namePtr = BytePointer(nameBuf) // we should only need a space for pointer...
                println(namePtr.address())
                println("libremidi_midi_out_port_name returned " + library.libremidi_midi_out_port_name(port, namePtr, size))
                println(namePtr.address())
                println("name size: " + size.get())
                name = ByteArray(size.get().toInt())
                namePtr.asBuffer().get(name)
            } else {
                name = ByteArray(1024)
                println("libremidi_midi_out_port_name returned " + library.libremidi_midi_out_port_name(port, name, size))
            }
            name = name.take(size.get().toInt()).toByteArray()
            println("name binary dump: ${name.joinToString(" ") { it.toString(16) }}")
            println(name.decodeToString())
        }
    }
    println("outputs:")
    println("libremidi_midi_observer_enumerate_output_ports returned: " + library.libremidi_midi_observer_enumerate_output_ports(observer, null, cb))
    println("done")
}
