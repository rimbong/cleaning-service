package com.boot.cleanhub.interceptor;

import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.HandlerInterceptor;

import com.boot.cleanhub.common.dto.PBox;
import com.boot.cleanhub.common.dto.PSession;

/**
 * <pre>
 * Controller 호출 전 Handler를 통한 Filter Interceptor Class
 * </pre>
 * 
 * @author in-seong Hwang 
 * 
 */
@Controller
public class EventCheckInterceptor implements HandlerInterceptor {

    @Autowired
    protected PSession session; // pSession 객체 선언

    // @Autowired
    // private CommonDaoIF commonDao;

    /**
     * <pre>
     *  Controller 호출전 실행 메소드
     * </pre>
     * 
     * @author in-seong Hwang     
     * @param request
     */
    @Override    
    public boolean preHandle(@NonNull HttpServletRequest request,@NonNull HttpServletResponse response,@NonNull Object handler) throws Exception {

        PBox pBox = new PBox();
        String uri = request.getRequestURI();
        Map<String, String[]> requestMap = request.getParameterMap();

        // [1] Request 객체로 부터 Parameter 정보를 PBox로 셋팅함.
        for ( Map.Entry<String,String[]> entry : requestMap.entrySet() ) {
            try {
                if ((entry.getValue() instanceof String[])) {
                    String[] values = (String[]) entry.getValue();

                    if (values != null && values.length == 1) {
                        pBox.set(entry.getKey(), values[0]);
                    } else {
                        pBox.set(entry.getKey(), values);
                    }
                } else {
                    pBox.set(entry.getKey(), entry.getValue());
                }
            } catch (Exception ex) {
            }            
        }

        // [2] Header 정보를 읽어와 PBox에 설정함.
        Enumeration<String> headerNames = request.getHeaderNames();
        PBox header = new PBox();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            header.set(headerName, request.getHeader(headerName));
        }
        pBox.setAll(header);

        // [3] 필요정보 pBox객체에 저장함
        pBox.set("serviceDomain", request.getScheme() + "://" + request.getServerName() + (request.getServerPort() == 80 ? "" : ":" + request.getServerPort()));
        pBox.set("remoteHost", request.getRemoteHost()); // Client Host
        pBox.set("remoteAddr", request.getRemoteAddr()); // Client Host IP
        pBox.set("remoteURI", uri); // Client 요청 URI
        pBox.set("remoteMethod", request.getMethod()); // Client 요청 Method (GET, POST)

        String remoteFullUrl;

        if (request.getQueryString() == null) {
            remoteFullUrl = request.getRequestURL().toString();

        } else {
            remoteFullUrl = request.getRequestURL() + (request.getQueryString().length() > 0 ? "?" : "") + request.getQueryString();
        }
        pBox.set("remoteFullURL", remoteFullUrl); // Client 요청 Full URL
        
        // [4] session에 저장된 회원정보를 pBox에 저장
        HttpSession session = request.getSession(false);
        if (session != null) {
            
            Enumeration<String> sessionNames = session.getAttributeNames();
            while (sessionNames.hasMoreElements()) {
                String sessionName = sessionNames.nextElement().toString();
                pBox.setIfEmpty(sessionName, session.getAttribute(sessionName));
            }
        }
        
       /* // [5] 메뉴 정보 조회
        if (!pBox.isEmpty("adminUserSeq")) {
            // [5-1] Top Menu 목록 조회
            PBoxList<PBox> topMenuList = commonDao.selectList("mybatis.common.main.mainMapper.selectTopMenuList", pBox);
            pBox.set("topMenuList", topMenuList);
            
            // [5-2] 전체 Sub Menu 목록 조회
            PBoxList<PBox> hideMenuList = new PBoxList<PBox>();
            
            PBox subParamBox = new PBox();
            subParamBox.set("adminUserSeq", pBox.get("adminUserSeq"));
            PBoxList<PBox> tmpSubMenuAllList = commonDao.selectList("mybatis.common.main.mainMapper.selectSubMenuList", subParamBox);
            
            Iterator<PBox> tmIT = topMenuList.iterator();
            while (tmIT.hasNext()) {
                PBox tmBox = tmIT.next();
                
                PBoxList<PBox> hideSubMenuList = new PBoxList<PBox>();
                
                Iterator<PBox> subIT = tmpSubMenuAllList.iterator();
                while (subIT.hasNext()) {
                    PBox subBox = subIT.next();
                    
                    if (tmBox.getInt("L1_MENU_SEQ") == subBox.getInt("L1_MENU_SEQ")) {
                        PBox subItemBox = new PBox();
                        
                        subItemBox.set("subMenuSeq", subBox.get("L2_MENU_SEQ"));
                        subItemBox.set("subMenuName", subBox.get("L2_MENU"));
                        subItemBox.set("subMenuUrl", subBox.get("L2_URL"));
                        
                        hideSubMenuList.add(subItemBox);
                    }
                }
                
                if (hideSubMenuList.size() > 0) {
                    PBox hideMenuBox = new PBox();
                    
                    hideMenuBox.set("topMenuSeq", tmBox.get("L1_MENU_SEQ"));
                    hideMenuBox.set("topMenuName", tmBox.get("L1_MENU"));
                    hideMenuBox.set("hideSubMenuList", hideSubMenuList);
                    
                    hideMenuList.add(hideMenuBox);
                }
            }
            
            // 숨겨진 메뉴 전체 목록이 존재하면 pBox에 저장
            if (hideMenuList.size() > 0) {
                pBox.set("hideMenuList", hideMenuList);
            }
            
            // [5-3] Sub Menu 목록 조회
            if (!pBox.isEmpty("currentTopMenuSeq")) {
                subParamBox.set("currentTopMenuSeq", pBox.get("currentTopMenuSeq"));
                
                PBoxList<PBox> subMenuList = commonDao.selectList("mybatis.common.main.mainMapper.selectSubMenuList", subParamBox);
                pBox.set("subMenuList", subMenuList);
                
                pBox.set("currentSubMenuSeq", pBox.isEmpty("currentSubMenuSeq") ? subMenuList.get(0).get("L2_MENU_SEQ") : pBox.get("currentSubMenuSeq")); // 현재 위치하는 Sub Menu Sequence 설정
            }
        }
        */
        // [8] 접속 browser 정보 및 버전 확인
        String deviceInfo = "";
        String browserType = "";
        String browserVersion = "";
        String browser = pBox.getString("user-agent");

        if (browser.contains("Googlebot")) {
            deviceInfo += "Googlebot ";
        } else if (browser.contains("Android")) {
            deviceInfo += "M-Android ";
        } else if (browser.contains("iPhone")) {
            deviceInfo += "M-IPhone ";
        } else if (browser.contains("iPod")) {
            deviceInfo += "M-IPhone ";
        } else if (browser.contains("Windows CE")) {
            deviceInfo += "M-WindowCE ";
        } else if (browser.contains("BlackBerry")) {
            deviceInfo += "M-BlackBerry ";
        } else if (browser.contains("BlackBerry")) {
            deviceInfo += "M-BlackBerry ";
        } else if (browser.contains("bingbot")) {
            deviceInfo += "bingbot ";
        } else if (browser.contains("AhrefsBot")) {
            deviceInfo += "AhrefsBot ";
        } else if (browser.contains("Yeti")) {
            deviceInfo += "Yeti ";
        } else if (browser.contains("linkdexbot")) {
            deviceInfo += "linkdexbot ";
        } else if (browser.contains("ZumBot")) {
            deviceInfo += "ZumBot ";
        } else if (browser.contains("archive.org_bot")) {
            deviceInfo += "archive.org_bot ";
        } else if (browser.contains("Exabot")) {
            deviceInfo += "Exabot ";
        } else if (browser.contains("Slurp")) {
            deviceInfo += "Yahoo-Slurp ";
        } else {
            deviceInfo += "PC ";
        }

        pBox.set("deviceInfo", (deviceInfo.startsWith("M-") ? "M" : "PC"));

        if (browser.contains("NAVER(inapp;")) {
            browserType += "NAVER";
            browserVersion += "NAVER";
        } else if (browser.contains("MSIE")) {
            browserType += "IE";
            browserVersion += "IE";
            if (browser.contains("Trident/4.0")) {
                browserType += "8";
                browserVersion += "8";
            } else if (browser.contains("Trident/5.0")) {
                browserType += "9";
                browserVersion += "9";
            } else if (browser.contains("Trident/6.0")) {
                browserType += "10";
                browserVersion += "10";
            } else if (browser.contains("MSIE 7")) {
                browserType += "7";
                browserVersion += "7";
            } else if (browser.contains("MSIE 6")) {
                browserType += "6";
                browserVersion += "6";
            }
        } else if (browser.contains("Firefox")) {
            browserType += "Firefox";
            browserVersion += "Firefox";
        } else if (browser.contains("Opera")) {
            browserType += "Opera";
            browserVersion += "Opera";
        } else if (browser.contains("Chrome")) {
            browserType += "Chrome";
            browserVersion += "Chrome";
        } else if (browser.contains("Safari")) {
            browserType += "Safari";
            browserVersion += "Safari";
        } else if (browser.contains("Trident/7.0")) {
            browserType += "IE 11";
            browserVersion += "IE 11";
        } else if (browser.contains("Dalvik")) {
            browserType += "Dalvik";
            browserVersion += "Dalvik";
        } else if (browser.contains("KAKAOTALK")) {
            browserType += "KAKAOTALK";
            browserVersion += "KAKAOTALK";
        } else if (browser.contains("facebookexternalhit")) {
            browserType += "facebook";
            browserVersion += "facebook";
        } else if (browser.contains("AndroidDownloadManager")) {
            browserType += "AndroidDownloadManager";
            browserVersion += "AndroidDownloadManager";
        } else {
            browserVersion += "Unknown Browser";
        }
        
        pBox.set("browserType",browserType);
        pBox.set("browserInfo", browserVersion);
        pBox.set("userAgent",browser);
		pBox.set("serverName",request.getServerName());		

        // 접속 HISTORY 
//        int insertCnt = commonDao.insert("mybatis.common.UtilMapper.insertUserAccessHistory", pBox);
//        if (insertCnt>0) {
//            System.out.println("insert history");
//        } else {
//            System.out.println("fail history");
//        }
		
        // [9] PAGE 메타 정보 조회        
        // pBox.set("pageMetaUrl", uri.substring(1));
        // PBox pageMetpBox = commonDao.select("mybatis.common.util.utilMapper.selectPageMetaInfo", pBox); 
        // if (pageMetpBox == null || pageMetpBox.isEmpty()) {
        //     pageMetpBox = new PBox(); // 왜 이게 NullPointerException이 뜰까...? 아예 set 메서드를 호출 못 함....
        //     pageMetpBox.set("URL", request.getRequestURL());
        //     pageMetpBox.set("TITLE", "Rimbongmall");
        // }
        // pBox.set("pageMetpBox", pageMetpBox);
        

        // [10] request 영역에 pBox를 저장하여 Controller로 보냄
        request.setAttribute("pBox", pBox);

        return true;
    }
}