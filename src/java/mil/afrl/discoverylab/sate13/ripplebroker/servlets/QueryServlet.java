package mil.afrl.discoverylab.sate13.ripplebroker.servlets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mil.afrl.discoverylab.sate13.ripplebroker.data.model.Patient;
import mil.afrl.discoverylab.sate13.ripplebroker.data.model.Vital;
import mil.afrl.discoverylab.sate13.ripplebroker.db.DatabaseHelper;
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

        String querytype = request.getParameter("QueryType");
        String restr;

        switch (Reference.QUERY_TYPES.valueOf(querytype.toUpperCase())) {
            case PATIENT:
                restr = this.processPatientQueryRequest(request);
                break;
            case VITAL:
                restr = this.processVitalQueryRequest(request);
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
        
        String type = "patient";
        String jstr = "{" + type + "s:{" + type + ":";

        List<Patient> pList = dbh.getAllpatients();

        if (pList != null && !pList.isEmpty()) {
            jstr += gson.toJson(pList) + "}}";
        } else {
            jstr += "[]}}";
        }

        return jstr;
    }

    protected String processVitalQueryRequest(HttpServletRequest request)
            throws ServletException, IOException {

        String type = "vital";
        String jstr = "{" + type + "s:{" + type + ":";

        String pidstr = request.getParameter("pid");
        String vidistr = request.getParameter("vidi");
        String limitstr = request.getParameter("limit");

        try {
            Integer pid = Integer.parseInt(pidstr);
            Integer vidi = Integer.parseInt(vidistr);
            Integer limit = Integer.parseInt(limitstr);

            List<Vital> vList = dbh.getAllVitalsForPatient(pid,
                                                           vidi,
                                                           limit);
            if (vList != null && !vList.isEmpty()) {
                jstr += gson.toJson(vList) + "}}";
            } else {
                jstr += "[]}}";
            }
        } catch (NumberFormatException nfe) {
            jstr = "{\"Failure\": \"invalid pid: " + pidstr
                   + ", vidi: " + vidistr + ", or limit: "
                   + limitstr + " params\"}";
        }

        return jstr;
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
