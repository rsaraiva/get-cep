package com.rsaraiva.labs.buscacep;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

@Path("/cep")
public class BuscaCepRest {
    
    private static final String URL_CORREIOS = "http://m.correios.com.br/movel/buscaCepConfirma.do";
    
    @Path("/{cep}")
    @GET
    @Produces("text/xml")
    public CepResult getCepResult(@PathParam("cep") String cep) {
        
        CepResult result = new CepResult();
        
        try {
            result = getLogradouro(result, cep);
            result = getCoordenadas(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return result;
    }
    
    private CepResult getLogradouro(CepResult cepInfo, String cep) {
        
        try {

            MultivaluedMap<String, String> map = new MultivaluedMapImpl();
            map.add("cepEntrada", cep);
            map.add("tipoCep", "");
            map.add("cepTemp", "");
            map.add("metodo", "buscarCep");

            WebResource resource = Client.create().resource(URL_CORREIOS);
            ClientResponse response = resource.type("application/x-www-form-urlencoded").post(ClientResponse.class, map);

            String output = response.getEntity(String.class);
            output = output.replaceAll("[\n\t]", "");
            output = output.replaceAll("[\\s]{2,100}", "");

            final String priceRegex = "<span class=\"respostadestaque\">([^<]+)</span>";

            Pattern pattern = Pattern.compile(priceRegex, Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(output);

            matcher.find();
            cepInfo.setLogradouro(matcher.group(1));

            matcher.find();
            cepInfo.setBairro(matcher.group(1));

            matcher.find();
            String cidade = matcher.group(1);
            String[] cidadeEstado = cidade.split("/");
            cepInfo.setCidade(cidadeEstado[0]);
            cepInfo.setUf(cidadeEstado[1]);

            matcher.find();
            cepInfo.setCep(matcher.group(1));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return cepInfo;
    }
    
    private CepResult getCoordenadas(CepResult cepInfo) {
        
        String endereco = cepInfo.getLogradouro() + "," + cepInfo.getBairro() + "," + cepInfo.getCidade() + "," + cepInfo.getUf();
        
        WebResource resource = Client.create().resource("https://maps.googleapis.com/maps/api/geocode/xml?address=" + endereco.replaceAll(" ", "%20") + "&sensor=false");
        ClientResponse response = resource.accept(MediaType.TEXT_XML).get(ClientResponse.class);
        String output = response.getEntity(String.class);
        
        {
            final String latRegex = "<geometry>[\\n\\s]*<location>[\\n\\s]*<lat>([0-9\\-\\.]+)</lat>[\\n\\s]*<lng>([0-9\\-\\.]+)</lng>";
            Pattern pattern = Pattern.compile(latRegex, Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(output);
            matcher.find(); 
            cepInfo.setLat(matcher.group(1));
            cepInfo.setLng(matcher.group(2));
        }
        
        return cepInfo;
    }
}
