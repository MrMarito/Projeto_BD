/** **
 * =============================================
 * ============== Bases de Dados ===============
 * ============== LEI  2020/2021 ===============
 * =============================================
 * =================== Demo ====================
 * =============================================
 * =============================================
 * === Department of Informatics Engineering ===
 * =========== University of Coimbra ===========
 * =============================================
 * <p>
 *
 * Authors: 
 *   Nuno Antunes <nmsa@dei.uc.pt>
 *   BD 2021 Team - https://dei.uc.pt/lei/
 */
package pt.uc.dei.bd2021;

import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.WeakKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;



@RestController
public class BDDemo {
    byte[] secret = Base64.getDecoder().decode("fSTM4LrTnDIss1xSho3MB3kRhBZCwZ6VVrlPLRH7coM=");

    private static final Logger logger = LoggerFactory.getLogger(BDDemo.class);

    private static java.sql.Timestamp getCurrentTimeStamp() {
        java.util.Date today = new java.util.Date();
        return new java.sql.Timestamp(today.getTime());
    }

    private boolean oAuth(String token){
        logger.info("###              oAuth in progress               ###");
        String[] tokenInfo = token.split(" ");

        Jws<Claims> result;
        if(tokenInfo[0].equals("Bearer")){
            try{
                result = Jwts.parserBuilder() // Rebuild the token for validation
                        .setSigningKey(Keys.hmacShaKeyFor(secret))
                        .build()
                        .parseClaimsJws(tokenInfo[1]);
            } catch (ExpiredJwtException e) {
                return false;
            } catch (UnsupportedJwtException e) {
                return false;
            } catch (MalformedJwtException e) {
                return false;
            } catch (SignatureException e) {
                return false;
            } catch (IllegalArgumentException e) {
                return false;
            } catch (WeakKeyException e) {
                return false;
            }

            logger.debug("Username from token -> " + result.getBody().getSubject());
            logger.debug("ID from token -> " + result.getBody().getAudience());

            if(result.getBody().getSubject() != null)
                return true;

        }
        return false;
    }

    private int getIdFromToken(String token){
        String[] tokenInfo = token.split(" ");

        Jws<Claims> result;
        try{
            result = Jwts.parserBuilder() // Rebuild the token
                    .setSigningKey(Keys.hmacShaKeyFor(secret))
                    .build()
                    .parseClaimsJws(tokenInfo[1]);

            return Integer.parseInt(result.getBody().getAudience());
        } catch (ExpiredJwtException e) {
            e.printStackTrace();
        } catch (UnsupportedJwtException e) {
            logger.error("Error in Token", e);
        } catch (MalformedJwtException e) {
            logger.error("Error in Token", e);
        } catch (SignatureException e) {
            logger.error("Error in Token", e);
        } catch (IllegalArgumentException e) {
            logger.error("Error in Token", e);
        } catch (WeakKeyException e) {
            logger.error("Error in Token", e);
        }

        return 0;
    }

    @GetMapping("/")
    public String landing() {
        return "Hello World!  <br/>\n"
                + "<br/>\n"
                + "Check the sources for instructions on how to use the endpoints!<br/>\n"
                + "<br/>\n"
                + "BD 2021<br/>\n"
                + "<br/>";
    }


    @PutMapping(value = "/dbproj/user", consumes = "application/json")
    @ResponseBody
    public Map<String, Object> authentication(
            @RequestBody Map<String, Object> payload
    ) {

        logger.info("###              PUT /dbproj/user               ###");
        Map<String, Object> content = new HashMap<>();

        if (!payload.containsKey("username") || !payload.containsKey("password")) {
            logger.warn("user and password are required to authenticate");
            content.put("erro", "AuthError");
            return content;
        }

        logger.info("---- AUTHENTICATION  ----");
        logger.debug("content: {}", payload);
        Connection conn = RestServiceApplication.getConnection();
        if (conn == null) {
            content.put("erro", "DB Problem!");
            return content;
        }

        String username = String.valueOf(payload.get("username"));
        String password = String.valueOf(payload.get("password"));


        try (Statement stmt = conn.createStatement()) {
            ResultSet rows = stmt.executeQuery("SELECT user_id, username, password FROM customer");
            logger.debug("---- Search in users  ----");
            while (rows.next()) {
                logger.debug("'user_id': {}, 'username': {}, 'password': {}", rows.getString("user_id"),
                        rows.getString("username"), rows.getString("password"));

                if(rows.getString("username").equals(username) && rows.getString("password").equals(password)){
                    Instant instant = Instant.now();

                    String jwt = Jwts.builder() // Create the jwt
                            .setSubject(username)
                            .setAudience(rows.getString("user_id"))
                            .setIssuedAt(Date.from(instant))
                            .setExpiration(Date.from(instant.plus(3, ChronoUnit.DAYS)))
                            .signWith(Keys.hmacShaKeyFor(secret))
                            .compact();

                    content.put("AuthToken",jwt);
                    return content;
                }
            }
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
        }
        content.put("erro", "AuthError");
        return content;
    }





    @PostMapping(value = "/dbproj/user", consumes = "application/json")
    @ResponseBody
    public Map<String, Object> createUser(
            @RequestBody Map<String, Object> payload
    ) {

        logger.info("###              POST /dbproj/user              ###");
        Connection conn = RestServiceApplication.getConnection();

        logger.debug("---- new user  ----");
        logger.debug("payload: {}", payload);

        Map<String, Object> content = new HashMap<>(); // Return Content

        int addId = 1;
        try (Statement stmt = conn.createStatement()) {
            ResultSet row = stmt.executeQuery("SELECT MAX(user_id) FROM customer");
            while(row.next()) {
                addId = row.getInt(1) + 1;
                logger.debug("ID GENERATED: " + addId);
            }

        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
            content.put("erro", ex.getMessage());
        }

        try (PreparedStatement ps = conn.prepareStatement(""
                + "INSERT INTO customer (user_id, username, password, email) "
                + "         VALUES (?, ?,   ? ,    ? )")) {

            ps.setInt(1, addId);
            ps.setString(2, (String) payload.get("username"));
            ps.setString(3, (String) payload.get("password"));
            ps.setString(4, (String) payload.get("email"));
            int affectedRows = ps.executeUpdate();
            conn.commit();
            if (affectedRows == 1) {
                content.put("userid", addId);
                return content;
            }
        }
        catch (SQLException ex) {
            content.put("erro", ex.getMessage());
            logger.error("Error in DB", ex);

            try {
                conn.rollback();
            } catch (SQLException ex1) {
                logger.warn("Couldn't rollback", ex);
            }
        } finally {
            try {
                conn.close();
            } catch (SQLException ex) {
                logger.error("Error in DB", ex);
                content.put("erro", ex.getMessage());
            }
        }
        return content;
    }


    @PostMapping(value = "/dbproj/leilao", consumes = "application/json")
    @ResponseBody
    public Map<String, Object> createAuction(
            @RequestBody Map<String, Object> payload, @RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, Object> content = new HashMap<>();
        logger.info("###              POST /dbproj/leilao              ###");

        if(token == null) {
            content.put("erro", "Auth Required");
            return content;
        }

        if(!oAuth(token)){
            content.put("erro", "Auth Error");
            return content;
        }

        int authId = getIdFromToken(token);

        Connection conn = RestServiceApplication.getConnection();


        logger.debug("---- new auction  ----");
        logger.debug("payload: {}", payload);

        // get auction id
        int addId = 1;
        try (Statement stmt = conn.createStatement()) {
            ResultSet row = stmt.executeQuery("SELECT MAX(id) FROM auction_product");
            while(row.next()) {
                addId = row.getInt(1) + 1;
                logger.debug("ID GENERATED: " + addId);
            }

        } catch (SQLException ex) {
            content.put("erro", ex.getMessage());
            logger.error("Error in DB", ex);
        }


        try (PreparedStatement ps = conn.prepareStatement(""
                + "INSERT INTO auction_product (id, start_price, end_time, title, description, product_id, product_name, customer_user_id, expired) "
                + "         VALUES (?, ?,   ? ,    ?, ?, ?, ?, ?,? )")) {

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            java.util.Date payloadDate = dateFormat.parse((String) payload.get("date"));
            java.sql.Date payloadDateSql = new java.sql.Date(payloadDate.getTime());
            Timestamp payloadTimestamp = new java.sql.Timestamp(payloadDateSql.getTime());

            Timestamp now = getCurrentTimeStamp();
            if(now.compareTo(payloadTimestamp)>0){ // end_time in the past
                content.put("erro", "Tempo invalido");
                return content;
            }


            ps.setInt(1, addId);
            ps.setObject(2,  payload.get("startPrice"), java.sql.Types.FLOAT);
            ps.setTimestamp(3, payloadTimestamp);
            ps.setString(4, (String) payload.get("title"));
            ps.setString(5, (String) payload.get("description"));
            ps.setInt(6, (int) payload.get("productId"));
            ps.setString(7, "Nome do produto");
            ps.setInt(8, authId);
            ps.setBoolean(9, false);
            int affectedRows = ps.executeUpdate();
            conn.commit();
            if (affectedRows == 1) {
                content.put("leilaoId", addId);
                return content;
            }
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
            content.put("erro", ex.getMessage());
            try {
                conn.rollback();
            } catch (SQLException ex1) {
                logger.warn("Couldn't rollback", ex1);
                content.put("erro", ex.getMessage());
            }
        } catch (ParseException ex) {
            logger.error("Error in Timestamp", ex);
            content.put("erro", ex.getMessage());
        } finally {
            try {
                conn.close();
            } catch (SQLException ex) {
                logger.error("Error in DB", ex);
                content.put("erro", ex.getMessage());
            }
        }
        return content;
    }


    @PostMapping(value = "/dbproj/mensagem/mural", consumes = "application/json")
    @ResponseBody
    public Map<String, Object> createMessage(
            @RequestBody Map<String, Object> payload, @RequestHeader(value = "Authorization", required = false) String token
    ) {
        Map<String, Object> content = new HashMap<>();
        logger.info("###              POST /dbproj/mensagem/mural              ###");

        if(token == null) {
            content.put("erro", "Auth Required");
            return content;
        }

        if(!oAuth(token)){
            content.put("erro", "Auth Error");
            return content;
        }

        int authId = getIdFromToken(token);

        Connection conn = RestServiceApplication.getConnection();

        logger.debug("---- new message  ----");
        logger.debug("payload: {}", payload);

        int addId = 1;
        try (Statement stmt = conn.createStatement()) {
            ResultSet row = stmt.executeQuery("SELECT MAX(message_id) FROM message");
            while(row.next()) {
                addId = row.getInt(1) + 1;
                logger.debug("ID GENERATED: " + addId);
            }

        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
            content.put("erro", ex.getMessage());
        }


        try (PreparedStatement ps = conn.prepareStatement(""
                + "INSERT INTO message (message_id, message, message_time, customer_user_id, auction_product_id) "
                + "         VALUES (?, ?,   ? ,    ?, ?)")) {


            ps.setInt(1, addId);
            ps.setString(2, (String) payload.get("message"));
            ps.setTimestamp(3, getCurrentTimeStamp());
            ps.setInt(4, authId);
            ps.setInt(5, (int) payload.get("auctionId"));
            int affectedRows = ps.executeUpdate();
            conn.commit();
            if (affectedRows == 1) {
                content.put("messageId", addId);
                return content;
            }
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
            content.put("erro", ex.getMessage());
            try {
                conn.rollback();
            } catch (SQLException ex1) {
                logger.warn("Couldn't rollback", ex1);
                content.put("erro", ex.getMessage());
            }
        } finally {
            try {
                conn.close();
            } catch (SQLException ex) {
                logger.error("Error in DB", ex);
                content.put("erro", ex.getMessage());
            }
        }
        return content;
    }


    @PostMapping(value = "/dbproj/licitar", consumes = "application/json")
    @ResponseBody
    public Map<String, Object> createBid(
            @RequestBody Map<String, Object> payload, @RequestHeader(value = "Authorization", required = false) String token
    ) {
        Map<String, Object> content = new HashMap<>();
        logger.info("###              POST /dbproj/licitar              ###");

        if(token == null) {
            content.put("erro", "Auth Required");
            return content;
        }

        if(!oAuth(token)){
            content.put("erro", "Auth Error");
            return content;
        }

        int authId = getIdFromToken(token);

        Connection conn = RestServiceApplication.getConnection();

        logger.debug("---- new bid  ----");
        logger.debug("payload: {}", payload);

        // Get bid id
        int addId = 1;
        try (Statement stmt = conn.createStatement()) {
            ResultSet row = stmt.executeQuery("SELECT MAX(bid_id) FROM bid");
            while(row.next()) {
                addId = row.getInt(1) + 1;
                logger.debug("ID GENERATED: " + addId);
            }

        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
            content.put("erro", ex.getMessage());
        }


        String payloadBidString = String.valueOf(payload.get("bid"));
        float payloadBid = Float.parseFloat(payloadBidString);
        int payloadId = (int) payload.get("auctionId");
        float maxPrice = 0;
        float originalPrice = 0;
        Timestamp originalTimestamp = null;
        int highestBidUserId = 1;

        logger.debug("Auction Id : " + payloadId);

        try (PreparedStatement ps = conn.prepareStatement("SELECT start_price, end_time FROM auction_product "
                + "WHERE id = ?")) { // Find Auction
            ps.setInt(1, payloadId);

            ResultSet row = ps.executeQuery();
            while(row.next()) {
                originalPrice = row.getFloat(1);
                originalTimestamp = row.getTimestamp(2);
                logger.debug("ORIGINAL PRICE FOUND: " + originalPrice);
                logger.debug("ORIGINAL TIMESTAMP FOUND: " + originalTimestamp);
            }
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
            content.put("erro", ex.getMessage());
        }

        if(originalPrice == 0){ // Auction with payload id does not exist
            content.put("erro", "O leilao com este id nao existe");
            return content;
        }

        Timestamp now = getCurrentTimeStamp();
        if(now.compareTo(originalTimestamp)>0){ // Bid already ended
            content.put("erro", "O leilao ja terminou.");
            return content;
        }


        // Get current price from auction
        try (PreparedStatement ps = conn.prepareStatement("SELECT MAX(price) FROM bid "
                + "WHERE auction_product_id = ?")) {
            ps.setInt(1, payloadId);

            ResultSet row = ps.executeQuery();
            while(row.next()) {
                maxPrice = row.getFloat(1);
                logger.debug("CURRENT PRICE FOUND: " + maxPrice);
            }

        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
            content.put("erro", ex.getMessage());
        }


        if(maxPrice == 0) { // No bids yet
            if(payloadBid < originalPrice){ // Bid is too low
                content.put("erro", "A bid tem um valor inferior ao necessario.");
                return content;
            }
        }
        else{ // At least one bid already
            if(payloadBid <= maxPrice) { // Bid is too low
                content.put("erro", "A bid tem um valor inferior ao necessario.");
                return content;
            }
        }

        // Get userId from highest bid
        try (PreparedStatement ps = conn.prepareStatement("SELECT customer_user_id FROM bid "
                + "WHERE auction_product_id = ? AND price = ?")) {
            ps.setInt(1, payloadId);
            ps.setFloat(2, maxPrice);

            ResultSet row = ps.executeQuery();
            while(row.next()) {
                highestBidUserId = row.getInt(1);
                logger.debug("HIGHEST BID USER ID: " + highestBidUserId);
            }

        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
            content.put("erro", ex.getMessage());
        }


        // Send new message (Bid exceeded)
        logger.debug("---- new message  ----");

        int messageId = 1;
        try (Statement stmt = conn.createStatement()) {
            ResultSet row = stmt.executeQuery("SELECT MAX(message_id) FROM message");
            while(row.next()) {
                messageId = row.getInt(1) + 1;
                logger.debug("MESSAGE ID GENERATED: " + messageId);
            }
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
            content.put("erro", ex.getMessage());
        }

        try (PreparedStatement ps = conn.prepareStatement(""
                + "INSERT INTO message (message_id, message, message_time, customer_user_id, auction_product_id) "
                + "         VALUES (?, ?,   ? ,    ?, ?)")) {

            ps.setInt(1, messageId);
            ps.setString(2, "A sua licitacao foi ultrapassada!");
            ps.setTimestamp(3, getCurrentTimeStamp());
            ps.setInt(4, highestBidUserId);
            ps.setInt(5, (int) payload.get("auctionId"));
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
            content.put("erro", ex.getMessage());
            try {
                conn.rollback();
            } catch (SQLException ex1) {
                logger.warn("Couldn't rollback", ex1);
                content.put("erro", ex.getMessage());
            }
        }


        // Create new Bid
        try (PreparedStatement ps = conn.prepareStatement(""
                + "INSERT INTO bid (bid_id, price, bid_time, auction_product_id, customer_user_id) "
                + "         VALUES (?, ?,   ? ,    ?, ?)")) {


            ps.setInt(1, addId);
            ps.setObject(2,  payload.get("bid"), java.sql.Types.FLOAT);
            ps.setTimestamp(3, getCurrentTimeStamp());
            ps.setInt(4, (int) payload.get("auctionId"));
            ps.setInt(5, authId);
            int affectedRows = ps.executeUpdate();
            conn.commit();
            if (affectedRows == 1) {
                content.put("bidId", addId);
                return content;
            }
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
            content.put("erro", ex.getMessage());
            try {
                conn.rollback();
            } catch (SQLException ex1) {
                logger.warn("Couldn't rollback", ex1);
                content.put("erro", ex.getMessage());
            }
        } finally {
            try {
                conn.close();
            } catch (SQLException ex) {
                logger.error("Error in DB", ex);
                content.put("erro", ex.getMessage());
            }
        }
        return content;

    }









    @GetMapping(value = "/dbproj/leiloes", produces = "application/json")
    @ResponseBody
    public List<Map<String, Object>> getAllAuctions() {
        logger.info("###              GET /leiloes              ###");
        Connection conn = RestServiceApplication.getConnection();
        List<Map<String, Object>> payload = new ArrayList<>();

        try (Statement stmt = conn.createStatement()) {
            ResultSet rows = stmt.executeQuery("SELECT id, description FROM auction_product");
            logger.debug("---- auctions  ----");
            while (rows.next()) {
                Map<String, Object> content = new HashMap<>();
                logger.debug("'id': {}, 'description': {}", rows.getInt("id"), rows.getString("description"));
                content.put("leilaoId", rows.getInt("id"));
                content.put("description", rows.getString("description"));
                payload.add(content);
            }
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
        }
        return payload;
    }


    @GetMapping(value = "/dbproj/leilao/{leilaoid}", produces = "application/json")
    @ResponseBody
    public List<Map<String, Object>> getAuctionDetails(@PathVariable String leilaoid) {
        logger.info("###              GET /auction             ###");
        Connection conn = RestServiceApplication.getConnection();
        List<Map<String, Object>> payload = new ArrayList<>();

        try (Statement stmt = conn.createStatement()) {
            ResultSet rows = stmt.executeQuery("SELECT id, description, end_time FROM auction_product");
            logger.debug("---- Auctions  ----");
            while (rows.next()) {
                Map<String, Object> content = new HashMap<>();
                logger.debug("'id': {}, 'description': {}, 'end_time': {}",
                        rows.getInt("id"), rows.getString("description"), rows.getString("end_time"));
                String ids = String.valueOf(rows.getInt("id"));
                if(leilaoid.equals(ids)){
                    content.put("id", rows.getInt("id"));
                    content.put("description", rows.getString("description"));
                    content.put("end_time", rows.getString("end_time"));
                    payload.add(content);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
        }
        try (Statement stmt = conn.createStatement()) {
            ResultSet rows2 = stmt.executeQuery("SELECT auction_product_id, message, message_time, customer_user_id FROM message");
            logger.debug("---- Messages  ----");
            while (rows2.next()) {
                Map<String, Object> content = new HashMap<>();
                logger.debug("'auction_product_id': {}, 'message': {}, 'message_time': {}, 'customer_user_id': {}",
                        rows2.getInt("auction_product_id"), rows2.getString("message"),
                        rows2.getString("message_time"), rows2.getInt("customer_user_id"));
                String auction_product_ids = String.valueOf(rows2.getInt("auction_product_id"));
                if(leilaoid.equals(auction_product_ids)){
                    if(!rows2.getString("message").equals("A sua licitacao foi ultrapassada!")) {
                        content.put("message", rows2.getString("message"));
                        content.put("message_time", rows2.getString("message_time"));
                        content.put("customer_user_id", rows2.getInt("customer_user_id"));
                        payload.add(content);
                    }
                }
            }
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
        }
        try (Statement stmt = conn.createStatement()) {
            ResultSet rows3 = stmt.executeQuery("SELECT price, bid_time, auction_product_id, customer_user_id FROM bid");
            logger.debug("---- Bids  ----");
            while (rows3.next()) {
                Map<String, Object> content = new HashMap<>();
                logger.debug("'price': {}, 'bid_time': {}, 'auction_product_id': {}, 'customer_user_id': {}",
                        rows3.getFloat("price"), rows3.getString("bid_time"),
                        rows3.getInt("auction_product_id"), rows3.getInt("customer_user_id"));
                String auction_product_ids2 = String.valueOf(rows3.getInt("auction_product_id"));
                if(leilaoid.equals(auction_product_ids2)){
                    content.put("bid", rows3.getFloat("price"));
                    content.put("bid_time", rows3.getString("bid_time"));
                    content.put("customer_user_id", rows3.getInt("customer_user_id"));
                    payload.add(content);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
        }
        return payload;
    }


    @GetMapping(value = "/dbproj/leiloes/{keyword}", produces = "application/json")
    @ResponseBody
    public List<Map<String, Object>> searchAuction(@PathVariable String keyword) {
        logger.info("###              GET /auction              ###");
        Connection conn = RestServiceApplication.getConnection();
        List<Map<String, Object>> payload = new ArrayList<>();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rows = stmt.executeQuery("SELECT id, description, product_id FROM auction_product");
            logger.debug("---- auction  ----");
            while (rows.next()) {
                Map<String, Object> content = new HashMap<>();
                logger.debug("'id': {}, 'description': {}, 'product_id': {}",
                        rows.getInt("id"), rows.getString("description"), rows.getInt("product_id"));
                String product_ids = String.valueOf(rows.getInt("product_id"));
                if(keyword.equals(rows.getString("description")) || keyword.equals(product_ids)){
                    content.put("id", rows.getInt("id"));
                    content.put("description", rows.getString("description"));
                    payload.add(content);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
        }
        return payload;
    }


    @GetMapping(value = "/dbproj/leilao_info/{userid}", produces = "application/json")
    @ResponseBody
    public List<Map<String, Object>> listAuctionUser(@PathVariable String userid) {
        logger.info("###              GET /auction              ###");
        Connection conn = RestServiceApplication.getConnection();
        List<Map<String, Object>> payload = new ArrayList<>();
        ArrayList<String> auctions = new ArrayList<String>();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rows = stmt.executeQuery("SELECT customer_user_id, auction_product_id FROM bid");
            logger.debug("---- auction  ----");
            while (rows.next()) {
                logger.debug("'customer_user_id': {}, 'auction_product_id': {}",
                        rows.getInt("customer_user_id"), rows.getInt("auction_product_id"));
                String customer_user_ids = String.valueOf(rows.getInt("customer_user_id"));
                if(userid.equals(customer_user_ids)){
                    String auction_product_ids = String.valueOf(rows.getInt("auction_product_id"));
                    auctions.add(auction_product_ids);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
        }
        try (Statement stmt = conn.createStatement()) {
            ResultSet rows2 = stmt.executeQuery("SELECT id, start_price, end_time, title, description, product_name, customer_user_id FROM auction_product");
            logger.debug("---- auction  ----");
            while (rows2.next()) {
                Map<String, Object> content = new HashMap<>();
                logger.debug("'id': {}, 'start_price': {}, 'end_time': {}, 'title': {}, 'description': {}, 'product_name': {}, 'customer_user_id': {}",
                        rows2.getInt("id"), rows2.getFloat("start_price"), rows2.getString("end_time"), rows2.getString("title"),
                        rows2.getString("description"), rows2.getString("product_name"), rows2.getInt("customer_user_id"));
                String customer_user_ids2 = String.valueOf(rows2.getInt("customer_user_id"));
                String ids = String.valueOf(rows2.getInt("id"));
                if(userid.equals(customer_user_ids2) || auctions.contains(ids)){
                    content.put("id", rows2.getInt("id"));
                    content.put("start_price", rows2.getFloat("start_price"));
                    content.put("end_time", rows2.getString("end_time"));
                    content.put("title", rows2.getString("title"));
                    content.put("description", rows2.getString("description"));
                    content.put("product_name", rows2.getString("product_name"));
                    payload.add(content);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
        }
        return payload;
    }


    @GetMapping(value = "/dbproj/inbox/{userid}", produces = "application/json")
    @ResponseBody
    public List<Map<String, Object>> getMessages(@PathVariable String userid) {
        logger.info("###              GET /inbox/{userid}              ###");
        Connection conn = RestServiceApplication.getConnection();
        List<Map<String, Object>> payload = new ArrayList<>();
        ArrayList<String> auctions = new ArrayList<>();
        ArrayList<String> auctions2 = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT customer_user_id, auction_product_id, message, message_time FROM message" + " WHERE customer_user_id = ?")) {
            ps.setInt(1, Integer.parseInt(userid));
            logger.debug("---- message  ----");
            ResultSet rows = ps.executeQuery();
            while (rows.next()) {
                Map<String, Object> content = new HashMap<>();
                logger.debug("'customer_user_id': {}, 'auction_product_id': {}, 'message': {}, 'message_time': {}",
                        rows.getInt("customer_user_id"), rows.getInt("auction_product_id"),
                        rows.getString("message"), rows.getString("message_time"));
                String auction_product_ids = String.valueOf(rows.getInt("auction_product_id"));
                if(rows.getString("message").equals("A sua licitacao foi ultrapassada!")){
                    content.put("type", "Bid Ultrapassada");
                    content.put("message", rows.getString("message"));
                    content.put("message_time", rows.getString("message_time"));
                    content.put("customer_user_id", rows.getInt("customer_user_id"));
                    content.put("auction_product_id", rows.getString("auction_product_id"));
                    payload.add(content);
                }
                else{
                    auctions.add(auction_product_ids);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT customer_user_id, id FROM auction_product WHERE customer_user_id = ?")) {
            ps.setInt(1, Integer.parseInt(userid));
            ResultSet rows = ps.executeQuery();
            logger.debug("---- message  ----");
            while (rows.next()) {
                logger.debug("'customer_user_id': {}, 'id': {}",
                        rows.getInt("customer_user_id"), rows.getInt("id"));
                String ids = String.valueOf(rows.getInt("id"));
                auctions.add(ids);
                auctions2.add(ids);
            }
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
        }
        try (Statement stmt = conn.createStatement()) {
            ResultSet rows2 = stmt.executeQuery("SELECT message, message_time, customer_user_id, auction_product_id FROM message");
            logger.debug("---- messages  ----");
            while (rows2.next()) {
                Map<String, Object> content = new HashMap<>();
                logger.debug("'message': {}, 'message_time': {}, 'customer_user_id': {}, 'auction_product_id': {}",
                        rows2.getString("message"), rows2.getString("message_time"),
                        rows2.getInt("customer_user_id"), rows2.getInt("auction_product_id"));
                String auction_product_ids = String.valueOf(rows2.getInt("auction_product_id"));
                if(auctions.contains(auction_product_ids) && !rows2.getString("message").equals("A sua licitacao foi ultrapassada!")){
                    if(auctions2.contains(auction_product_ids)){
                        content.put("type", "Messagem de um leilao proprio");
                    }
                    else{
                        content.put("type", "Messagem de um leilao onde escreveu");
                    }
                    content.put("message", rows2.getString("message"));
                    content.put("message_time", rows2.getString("message_time"));
                    content.put("customer_user_id", rows2.getInt("customer_user_id"));
                    content.put("auction_product_id", rows2.getString("auction_product_id"));
                    payload.add(content);
                }
            }
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
        }
        return payload;
    }




    @PutMapping(value = "/dbproj/leilao/{leilaoid}", consumes = "application/json")
    @ResponseBody
    public String updateAuction(@RequestBody Map<String, Object> payload, @PathVariable String leilaoid) {
        logger.info("###              PUT /leilao/{leilaoid}               ###");
        if (!payload.containsKey("title") || !payload.containsKey("description")
                || !payload.containsKey("startPrice") || !payload.containsKey("date")) {
            logger.warn("more parameters are required to update");
            return "more parameters are required to update";
        }

        logger.info("---- update auction  ----");
        logger.debug("content: {}", payload);
        Connection conn = RestServiceApplication.getConnection();
        if (conn == null) {
            return "DB Problem!";
        }

        // For timestamp
        Timestamp payloadTimestamp = null;
        try{
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            java.util.Date payloadDate = dateFormat.parse((String) payload.get("date"));
            java.sql.Date payloadDateSql = new java.sql.Date(payloadDate.getTime());
            payloadTimestamp = new java.sql.Timestamp(payloadDateSql.getTime());
        } catch (ParseException ex) {
            logger.error("Error in Timestamp", ex);
        }


        // Get original auction data
        float originalPrice = 0;
        String originalTitle = "", originalDescription = "";
        Timestamp originalEnd_time = getCurrentTimeStamp();

        try (PreparedStatement ps = conn.prepareStatement("SELECT start_price, end_time, title, description FROM auction_product WHERE id = ?")) {
            ps.setInt(1, Integer.parseInt(leilaoid));
            logger.debug("---- original auction details  ----");
            ResultSet rows = ps.executeQuery();
            while (rows.next()) {
                originalPrice = rows.getFloat("start_price");
                originalEnd_time = rows.getTimestamp("end_time");
                originalTitle = rows.getString("title");
                originalDescription = rows.getString("description");
            }
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
        }


        // Update auction_product table
        int affectedRows = 0;
        try (PreparedStatement ps = conn.prepareStatement(""
                + "UPDATE auction_product"
                + "   SET start_price = ?," +
                "end_time = ?," +
                "title = ?," +
                "description = ?"
                + " WHERE id = ?")) {

            ps.setObject(1,  payload.get("startPrice"), java.sql.Types.FLOAT);
            ps.setTimestamp(2, payloadTimestamp);
            ps.setString(3, (String) payload.get("title"));
            ps.setString(4, (String) payload.get("description"));

            ps.setInt(5, Integer.parseInt(leilaoid));

            affectedRows = ps.executeUpdate();
            conn.commit();

        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
            try {
                conn.rollback();
            } catch (SQLException ex1) {
                logger.warn("Couldn't rollback", ex);
            }
        }


        // Get ID for Update table
        int updateId = 1;
        try (Statement stmt = conn.createStatement()) {
            ResultSet row = stmt.executeQuery("SELECT MAX(update_id) FROM updates");
            while(row.next()) {
                updateId = row.getInt(1) + 1;
                logger.debug("UPDATE ID GENERATED: " + updateId);
            }

        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
        }
        // Add new row in Update table
        try (PreparedStatement ps = conn.prepareStatement(""
                + "INSERT INTO updates (update_id, auction_product_id, start_price, end_time, title, description) "
                + "         VALUES (?, ?,   ? ,    ?, ?, ?)")) {

            ps.setInt(1, updateId);
            ps.setInt(2, Integer.parseInt(leilaoid));
            ps.setFloat(3,  originalPrice);
            ps.setTimestamp(4, originalEnd_time);
            ps.setString(5, originalTitle);
            ps.setString(6, originalDescription);

            ps.executeUpdate();
            conn.commit();

        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
            try {
                conn.rollback();
                return "Failed";
            } catch (SQLException ex1) {
                logger.warn("Couldn't rollback", ex);
                return "Failed";
            }
        }

        try {
            conn.close();
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
            return "Failed";
        }
        return "Updated: " + affectedRows;
    }







    @PutMapping(value = "/dbproj/term", consumes = "application/json")
    @ResponseBody
    public String updateAuctionState() {
        logger.info("###              PUT /Termino               ###");
        logger.info("---- update auction state ----");
        Connection conn = RestServiceApplication.getConnection();
        if (conn == null) {
            return "DB Problem!";
        }
        ArrayList<Integer> auctions = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT id, end_time FROM auction_product WHERE expired = ?")) {
            stmt.setBoolean(1, false);
            ResultSet rows = stmt.executeQuery();
            logger.debug("---- Auction  ----");
            while (rows.next()) {
                logger.debug("'id': {}, 'end_time': {}", rows.getInt("id"), rows.getString("end_time"));
                Timestamp ts1 = getCurrentTimeStamp();
                if(ts1.compareTo(rows.getTimestamp("end_time"))>0)
                    auctions.add(rows.getInt("id"));
            }
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
        }
        int updated = 0;
        for (Integer auction : auctions) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE auction_product SET expired = ? WHERE id = ?")) {
                logger.debug("---- auction  ----");
                ps.setBoolean(1, true);
                ps.setInt(2, auction);

                int affectedRows = ps.executeUpdate();
                conn.commit();
                updated += affectedRows;
            } catch (SQLException ex) {
                logger.error("Error in DB", ex);
                try {
                    conn.rollback();
                } catch (SQLException ex1) {
                    logger.warn("Couldn't rollback", ex);
                }
            }
        }
        try {
            conn.close();
            return "Updated: " + updated;
        } catch (SQLException ex) {
            logger.error("Error in DB", ex);
        }

        return "Failed";
    }



}
