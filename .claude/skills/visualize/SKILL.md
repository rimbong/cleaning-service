---
name: Visualize (HTML)
description: Generate self-contained, OFFLINE HTML visualizations (Mermaid diagrams) of the workspace — module dependency/architecture graph, build order, troubleshooting decision flow, worklog timeline — or convert a Markdown doc to styled HTML. For showing/reviewing work with others beyond .md files. Output to .claude/docs/visual/*.html.
when_to_use: 작업 내용을 다른 사람에게 보여주거나 리뷰할 때 시각적으로(HTML/다이어그램). "시각화", "HTML로", "다이어그램", "발표/세미나 자료".
allowed-tools: Read, Write, Edit, Glob, Grep, PowerShell, Skill, AskUserQuestion
argument-hint: [대상: arch|build|trouble|history|dashboard|<md경로>]
---

# Visualize (시각화 HTML 생성)

워크스페이스 작업을 **오프라인 자체완결 HTML(Mermaid 다이어그램)** 로 만든다. MD 외에 시각적으로 보여주거나 리뷰할 때 사용. 산출물은 `.claude/docs/visual/*.html`, JS는 `.claude/assets/` 로컬 번들 참조(인터넷 차단 환경에서도 작동, SVN 공유).

## 전제: 로컬 번들 자산 확인
- 필요한 JS: `.claude/assets/mermaid.min.js`, `.claude/assets/marked.min.js`(MD 변환용).
- 없으면 1회 다운로드:
  ```powershell
  $a="$env:CLAUDE_PROJECT_DIR\.claude\assets"; New-Item -ItemType Directory -Force $a | Out-Null
  Invoke-WebRequest "https://cdn.jsdelivr.net/npm/mermaid@10.9.1/dist/mermaid.min.js" -OutFile "$a\mermaid.min.js" -UseBasicParsing
  Invoke-WebRequest "https://cdn.jsdelivr.net/npm/marked@12.0.2/marked.min.js"        -OutFile "$a\marked.min.js"   -UseBasicParsing
  ```
  - 인터넷이 막혀 받을 수 없으면 → 사용자에게 두 파일을 `.claude/assets/` 에 넣어달라고 안내.
- **3MB짜리 mermaid를 HTML에 인라인하지 말 것**(SVN diff 오염). 항상 상대경로로 참조.
  - `docs/visual/x.html` 기준 경로 = `../../assets/mermaid.min.js`.

## 절차
1. **대상 결정**(인자 `$1` 또는 질문):
   - `arch` 모듈 의존성/아키텍처, `build` 빌드순서, `trouble` 트러블슈팅 흐름, `history` 작업이력 타임라인, `dashboard` 위 전부 한 페이지, `<md경로>` 임의 MD를 styled HTML로.
2. **데이터 수집(추측 금지)**:
   - 의존성/빌드순서 → `build-all.ps1 -DryRun` 결과 또는 각 pom `<dependency>` 파싱.
   - 트러블슈팅/이력 → `troubleshooting/*.md`, `worklog/*.md` 읽어 항목 추출.
3. **HTML 생성** → `.claude/docs/visual/<name>.html`.
   - 기존 `dashboard.html` 을 **레퍼런스 템플릿**으로 사용(헤더/sticky nav/section 카드/스타일/ mermaid init 블록).
   - `<head>` 에 `<script src="../../assets/mermaid.min.js"></script>`, 끝에 `mermaid.initialize({startOnLoad:true, theme:'default', securityLevel:'loose'})`.
   - 탭은 **앵커 네비(스크롤)** 방식 사용 — `display:none` 으로 숨기면 mermaid가 0폭으로 잘못 렌더되는 버그가 있으니 섹션은 항상 보이게 두고 nav로 점프.
4. **보고**: 산출 경로 + "브라우저로 더블클릭" 안내. (정적 HTML이라 서버 불필요)

## Mermaid 다이어그램 매핑(이 워크스페이스)
| 목적 | 종류 | 메모 |
|------|------|------|
| 모듈 의존/아키텍처 | `flowchart TD` + `subgraph`(계층) | 화살표=의존(빌드 선행). v2a는 `classDef` 강조 |
| 빌드 순서 | `flowchart LR` 체인 | 위상정렬 순서 |
| 트러블슈팅 | `flowchart TD` 분기 | 증상→조치, 조치 노드 `classDef fix` |
| 요청 흐름 | `sequenceDiagram` | AcceptFilter→Controller 등 |
| 작업 이력 | `timeline` | 날짜 : 작업 : 작업 … |

## 규칙 / 주의
- **오프라인 우선**: CDN 직참조 금지, 항상 로컬 번들.
- 문서 내 섹션 참조는 `§` 기호 대신 **"N번 섹션"** 한글 표기(워크스페이스 컨벤션과 일치).
- 한글 라벨 OK(`<meta charset="UTF-8">` 필수). Write 시 파일은 UTF-8.
- **민감정보(비밀번호·DB 계정·접속 IP) 넣지 말 것**(HTML이 공유됨).
- 다이어그램 라벨에 `(`,`)`,`:` 등 특수문자가 있으면 노드 텍스트를 `["..."]` 따옴표로 감쌀 것.
- 산출물·자산 모두 `.claude/` 아래라 SVN 팀 공유. 슬라이드(발표)용이 필요하면 reveal.js 변형 별도 검토.
- 생성/갱신 후 `/worklog` 로 기록.
