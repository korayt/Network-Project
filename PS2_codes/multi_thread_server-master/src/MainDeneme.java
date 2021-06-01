import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainDeneme {

    public static void main(String[] args) {
        getMarsWeather();
    }

    private static void downloadImage() {
        Pattern p = Pattern.compile("(?<=hdurl\":\").*?(?=\")");

        try {
            URL apodAPI = new URL("https://api.nasa.gov/planetary/apod?api_key=6Io3jtp4bh4PuQaVVxiQIzvcvB95xdBO6mSYhL2l");
            URLConnection apod = apodAPI.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(apod.getInputStream()));

            String inputLine = in.readLine();
            Matcher m = p.matcher(inputLine);
            System.out.println(m);
            if (m.find()) {
                System.out.println(m.group());
            }
            in.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void getMarsWeather() {
        try {
            URL insightAPI = new URL("https://api.nasa.gov/insight_weather/?api_key=6Io3jtp4bh4PuQaVVxiQIzvcvB95xdBO6mSYhL2l&feedtype=json&ver=1.0");
            URLConnection insight = insightAPI.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(insight.getInputStream()));
            String input = new String();
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                input+=inputLine;
                System.out.println(input);
            //Matcher m = p.matcher(inputLine);
            //System.out.println(m);
            //if (m.find()) {
            //    System.out.println(m.group());
            //}
            in.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

}
