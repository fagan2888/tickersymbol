/*
 * MIT License
 *
 * Copyright (c) 2018, Apptastic Software
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.apptastic.tickersymbol.provider;

import com.apptastic.tickersymbol.Source;
import com.apptastic.tickersymbol.TickerSymbol;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;


/**
 * Ticker provider implementation that fetches ticker information from Morning Star.
 * Morning Start is a investment research firm that compiles and analyzes fund, stock and general market data.
 */
public class MorningStar implements TickerSymbolProvider {
    private static final String URL = "http://www.morningstar.com/api/v2/search/securities/5/usquote-v2/?q=%1$s";
    private static final String HTTP_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.181 Safari/537.36";


    /**
     * Search ticker by ISIN code.
     * @param isin ISIN code.
     * @return stream of tickers
     * @throws IOException IO exception
     */
    public List<TickerSymbol> searchByIsin(String isin) throws IOException {
        String url = String.format(URL, isin);

        BufferedReader reader = sendRequest(url, "UTF-8");
        JsonReader jsonReader = new JsonReader(reader);

        return handleResponse(jsonReader, true);
    }


    private BufferedReader sendRequest(String url, String characterEncoding) throws IOException {

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
        connection.setRequestProperty("User-Agent", HTTP_USER_AGENT);

        connection.connect();
        InputStream inputStream = connection.getInputStream();

        if ("gzip".equals(connection.getContentEncoding()))
            inputStream = new GZIPInputStream(inputStream);

        return new BufferedReader(new InputStreamReader(inputStream, characterEncoding));
    }


    public List<TickerSymbol> handleResponse(JsonReader reader, boolean first) throws IOException {
        List<TickerSymbol> tickers = new ArrayList<>();

        if (reader.peek() == JsonToken.BEGIN_OBJECT)
            reader.beginObject();

        while (reader.hasNext()) {

            switch (reader.nextName()) {
                case "m":
                    parseM(reader, tickers);
                    continue;

                default:
                    reader.skipValue();
            }

        }

        if (reader.peek() == JsonToken.END_OBJECT)
            reader.endObject();

        return tickers;
    }


    private void parseM(JsonReader reader, List<TickerSymbol> tickers) throws IOException {
        if (reader.peek() == JsonToken.BEGIN_ARRAY)
            reader.beginArray();

        if (reader.peek() == JsonToken.BEGIN_OBJECT)
            reader.beginObject();

        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "r":
                    parseR(reader, tickers);
                    continue;

                default:
                    reader.skipValue();
            }
        }

        if (reader.peek() == JsonToken.END_OBJECT)
            reader.endObject();

        if (reader.peek() == JsonToken.END_ARRAY)
            reader.endArray();
    }


    private void parseR(JsonReader reader, List<TickerSymbol> tickers) throws IOException {
        if (reader.peek() == JsonToken.BEGIN_ARRAY)
            reader.beginArray();

        while (reader.hasNext()) {
            TickerSymbol ticker = parseTicker(reader);

            if (ticker != null)
                tickers.add(ticker);
        }

        if (reader.peek() == JsonToken.END_ARRAY)
            reader.endArray();
    }


    private TickerSymbol parseTicker(JsonReader reader) throws IOException {
        TickerSymbol ticker = null;

        if (reader.peek() == JsonToken.BEGIN_OBJECT) {
            reader.beginObject();

            ticker = new TickerSymbol();
            ticker.setSource(Source.MORNING_STAR);
        }

        while (reader.hasNext()) {

            switch (reader.nextName()) {
                case "OS001":
                    ticker.setSymbol(reader.nextString());
                    continue;

                case "OS01W":
                    ticker.setName(reader.nextString());
                    continue;

                case "OS05J":
                    ticker.setIsin(reader.nextString());
                    continue;

                case "OS05M":
                    ticker.setCurrency(reader.nextString());
                    continue;

                case "LS01Z":
                    ticker.setMic(reader.nextString());
                    continue;

                case "OS01X":
                    ticker.setDescription(reader.nextString());
                    continue;

                default:
                    reader.skipValue();
            }
        }

        if (reader.peek() == JsonToken.END_OBJECT)
            reader.endObject();

        return ticker;
    }
}
