# =====================================================================
# build.env.example.ps1 — build.ps1 이 읽는 "머신 전용" 설정 템플릿
#
# 사용법: 이 파일을 build.env.ps1 로 복사한 뒤 값을 채운다.
#   (build.env.ps1 은 .gitignore 됨 — 절대경로·SSL 우회는 커밋 금지)
# =====================================================================

# [필수] 빌드에 사용할 JDK 경로 (이 프로젝트는 Java 8)
$BUILD_JAVA_HOME = 'C:\path\to\jdk8'

# [선택] Maven 실행파일. 비워두면 프로젝트의 mvnw.cmd 사용.
# $BUILD_MVN_CMD = 'D:\path\to\apache-maven-3.9.11\bin\mvn.cmd'

# [선택] Maven settings.xml (-s). 사내 저장소/로컬repo 지정 시.
# $BUILD_MVN_SETTINGS = 'D:/path/to/conf/settings.xml'

# [선택] 추가 Maven 인자(배열). 사내망 SSL 가로채기(PKIX) 우회 등.
#   ※ 배열이라 PowerShell 인자 쪼갬 없음. 근본해결(프록시 CA를 JDK cacerts 등록) 후엔 비우세요.
# $BUILD_MVN_EXTRA_ARGS = @('-Daether.connector.https.securityMode=insecure')
