import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {

    @Test
    void saveMessage() {
        //given
        String mes = "From: rou\n" +
                "Text: Так, надеюсь тормоза такие только из-за лишнего sleep у клиента...\n" +
                "Message time: " + LocalDateTime.now();
        //when
        Server.saveMessage(mes);
        //then
        assertEquals(Server.extractLog().strip(), mes);
    }

    @Test
    void extractLog() {
        //given
        String emptyData = "";
        //when
        String serverLog = Server.extractLog();
        //then
        assertNotEquals(emptyData, serverLog);
    }

    @Test
    void cleaningMes() {
        //given
        LocalDateTime now = LocalDateTime.now();
        String rawMes = "From: rou*" +
                "Text: Так, надеюсь тормоза такие только из-за лишнего sleep у клиента...*" +
                "Message time: " + now;
        //when
        String clearMes = Server.cleaningMes(rawMes);
        //then
        assertEquals("From: rou\n" +
                "Text: Так, надеюсь тормоза такие только из-за лишнего sleep у клиента...\n" +
                "Message time: " + now, clearMes);
    }

    @Test
    void mesMaker() {
        //given
        String protoMes = "rou" + "###" +
                "Так, надеюсь тормоза такие только из-за лишнего sleep у клиента...\n";
        //when
        String mes = Server.mesMaker(protoMes).replaceAll("Message time: [0-9-T:.\n]+", "");
        //then
        assertEquals(mes, "From: rou\nText: Так, надеюсь тормоза такие только из-за лишнего sleep у клиента...\n");
    }
}