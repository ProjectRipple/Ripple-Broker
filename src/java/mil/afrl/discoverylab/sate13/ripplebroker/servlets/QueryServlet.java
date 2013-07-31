package mil.afrl.discoverylab.sate13.ripplebroker.servlets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mil.afrl.discoverylab.sate13.ripplebroker.data.model.Vital;
import mil.afrl.discoverylab.sate13.ripplebroker.db.DatabaseHelper;
import mil.afrl.discoverylab.sate13.ripplebroker.network.UDPPatientVitalStreamer;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Config;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.PATIENT_TABLE_COLUMNS;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.TABLE_NAMES;
import mil.afrl.discoverylab.sate13.ripplebroker.util.Reference.TableColumns;
import org.apache.log4j.Logger;

/**
 *
 * @author james
 */
public class QueryServlet extends HttpServlet {

    private static DatabaseHelper dbh;
    private static Logger log;
    private static Gson gson;
    private int check;

    @Override
    public void init() {
        dbh = DatabaseHelper.getInstance(null);
        log = Logger.getLogger(Config.LOGGER_NAME);
        gson = new GsonBuilder().setDateFormat(Reference.DATE_TIME_FORMAT).create();
        check = 0;
    }

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request,
                                  HttpServletResponse response)
            throws ServletException, IOException {

        log.debug("Processing request");

        response.setContentType("text/html;charset=UTF-8");

        addResponder(request.getRemoteAddr());

        String type = request.getParameter("QueryType");
        String querytype = (type == null) ? "" : type;
        String restr;

        switch (Reference.QUERY_TYPES.valueOf(querytype.toUpperCase())) {
            case PATIENT:
                restr = this.processPatientQueryRequest(request);
                break;
            case VITAL:
                restr = this.processVitalQueryRequest(request);
                break;
            case SUBSCRIPTION:
                restr = this.processSubscriptionQuery(request);
                break;
            default:
                restr = "{\"Failure\": \"invalid query type\"}";
        }

        PrintWriter out = response.getWriter();
        try {
            out.println(restr);
        } finally {
            out.close();
        }
        check++;
    }

    protected void addResponder(String ip_addr) {
        try {
            if (!dbh.patientExists(InetAddress.getByName(ip_addr))) {

                List<Map.Entry<TableColumns, String>> entries = new ArrayList<Map.Entry<TableColumns, String>>();
                entries.add(new AbstractMap.SimpleEntry<TableColumns, String>(PATIENT_TABLE_COLUMNS.IP_ADDR, ip_addr));
                dbh.insertRow(TABLE_NAMES.PATIENT, entries);
            }
        } catch (UnknownHostException ex) {
            log.error("Unable to parse responder IP address: "
                      + ip_addr + " error: " + ex);
        }
    }

    protected String processPatientQueryRequest(HttpServletRequest request)
            throws ServletException, IOException {

        String jstr = "";

        JsonArray patients = (JsonArray) gson.toJsonTree(dbh.getAllpatients());

        if (patients.size() > 0) {
            // Create root JSON object
            JsonObject json = new JsonObject();
            // Add vitals array to root object
            json.add("patients", patients);
            jstr = json.toString();
        }
        return jstr;
    }

    protected String processVitalQueryRequest(HttpServletRequest request)
            throws ServletException, IOException {

        String jstr = "";
        String pidstr = request.getParameter("pid");
        String vidistr = request.getParameter("vidi");
        String rlimstr = request.getParameter("rowlimit");
        String tlimstr = request.getParameter("timelimit");

        try {
            Integer pid = Integer.parseInt(pidstr);
            Integer vidi = Integer.parseInt(vidistr);
            Integer rowLimit = Integer.parseInt(rlimstr);
            Integer timeLimit = Integer.parseInt(tlimstr);

//            List<Vital> vList = dbh.getVitalsForPatient(pid,
//                                                        vidi,
//                                                        rowLimit,
//                                                        timeLimit);

            List<Vital> vList = dbh.getBufferedVitalsForPatient(pid,
                                                                vidi,
                                                                rowLimit,
                                                                timeLimit);

            JsonArray vitals = (JsonArray) gson.toJsonTree(vList);

            // Do something if vitals were found
            if (vitals.size() > 0) {
                // Create root JSON object
                JsonObject json = new JsonObject();
                // Add vitals array to root object
                json.add("vitals", vitals);
                // Add patient id to root object
                json.add("pid", gson.toJsonTree(pid, Integer.class));
                jstr = json.toString();
            }
        } catch (Exception e) {
            jstr = "{\"failure\": \"" + e + "\"}";
        }
        return jstr;
    }

    private String processSubscriptionQuery(HttpServletRequest request)
            throws ServletException, IOException {

        boolean res = true;

        Integer pid = -1;
        Integer port = -1;

        String pidstr = request.getParameter("pid");
        String actionstr = request.getParameter("action");
        String portstr = request.getParameter("port");
        String ex = "";

        JsonObject json = new JsonObject();

        try {
            pid = Integer.parseInt(pidstr);
            port = Integer.parseInt(portstr);


            InetAddress addr = InetAddress.getByName(request.getRemoteAddr());
            InetSocketAddress sockAddr = new InetSocketAddress(addr, port);

            if (actionstr.equals("subscribe")) {
                UDPPatientVitalStreamer.addSubscriber(pid, sockAddr);
            } else if (actionstr.equals("unsubscribe")) {
                UDPPatientVitalStreamer.removeSubscriber(pid, sockAddr);
            }
        } catch (Exception e) {
            ex = e.getMessage();
            res = false;
        }

        json.addProperty("success", res);
        json.addProperty("exception", ex);
        json.addProperty("pid_echo", pid);
        json.addProperty("action_echo", actionstr);
        json.addProperty("port_echo", port);

        return json.toString();
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
