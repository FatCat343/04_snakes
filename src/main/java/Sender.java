public class Sender {
    public String ip;
    public int port;
    Sender(String ip){
        this.ip = ip;
    }
    Sender(){

    }
    public boolean equals(Object obj){
        if (obj == null) return false;
        if (!Sender.class.isAssignableFrom(obj.getClass())){
            return false;
        }
        final Sender s = (Sender) obj;
        if (s.port == port && s.ip.equals(ip)) {
           // System.out.println("equal");
            return true;
        }
        else {
            //System.out.println("not equal");
            return false;
        }
    }
    public int hashCode() {
        return port + ip.hashCode();
    }
}
