import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientTest {

    @Test
    void mesMaker() {
        String nickName = "rou";
        String body = "Так, надеюсь тормоза такие только из-за лишнего sleep у клиента...";
        //when
        String mes = Client.mesMaker(nickName, body);
        //then
        assertEquals(mes, "rou*###*Так, надеюсь тормоза такие только из-за лишнего sleep у клиента...");
    }
}