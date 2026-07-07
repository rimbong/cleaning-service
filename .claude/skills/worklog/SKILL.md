---
name: worklog
description: Append a chronological work-history entry to the current author's monthly worklog file (.claude/docs/worklog/YYYY-MM-<author>.md) summarizing the code/troubleshooting work just completed (분류·증상·원인·조치·변경 파일·검증). Per-author files avoid SVN merge conflicts. Use after fixing a problem or finishing a work chunk. Triggers also when the user asks to "기록/이력 남겨/worklog".
when_to_use: 코드/트러블슈팅 작업을 마친 뒤 그 내용을 시간순 이력으로 남길 때. (Stop 훅이 변경 감지 시 권고)
allowed-tools: Read, Write, Edit, Grep, Glob, Bash, PowerShell
argument-hint: [한 줄 요약(선택)]
---

# Work History Logger (작업 이력 기록)

방금까지 진행한 코드/트러블슈팅 작업을 **"작성자 × 월" 파일**에 시간순으로 추가한다.
각자 자기 파일에만 써서 **SVN 머지 충돌을 피하는** 구조다.

## 파일 규칙
- 대상: `<project-root>\.claude\docs\worklog\<yyyy-MM>-<작성자>.md`
  - 작성자 = **고정값 `HIS`** (개인 프로젝트라 OS 사용자명 대신 고정 식별자 사용 — 회사명 미포함).
  - 예: `worklog/2026-06-HIS.md`
- **색인(`작업이력.md`)은 정적 안내문이다 — 건드리지 말 것.** 월/항목 목록을 거기 쌓으면 또 충돌난다.

## 절차
1. **변경 사실 파악**(추측 금지): 대화 맥락 요약 + `svn status`(없으면 `git status --porcelain`)로 실제 변경 파일 확인.
2. **값 취득**(PowerShell): 날짜 `Get-Date -Format "yyyy-MM-dd"`, 월 `Get-Date -Format "yyyy-MM"`. 작성자는 고정 `HIS`.
3. **대상 파일 결정**: `.claude\docs\worklog\<yyyy-MM>-<작성자>.md`.
   - 없으면 **새로 생성**(아래 "파일 헤더"). 색인은 갱신하지 않는다(정적).
4. **항목 작성** — 포맷:
   ```
   ### [분류] 제목
   - 증상: (있으면)
   - 원인: (있으면)
   - 조치: 무엇을 어떻게
   - 변경 파일: 핵심 경로(많으면 대표 + 개수)
   - 검증: 어떻게 확인했는지(있으면)
   - 관련: 이슈/문서 링크 (예: [이슈M](../troubleshooting/0X-....md))
   ```
   - `분류`: `수정`·`설정`·`환경`·`문서`·`리팩터` 중 택1. 인자 `$1` 있으면 제목 힌트.
5. **삽입 위치(파일 내)**:
   - `## YYYY-MM-DD` 섹션이 이미 있으면 그 섹션 **맨 아래**에 `###` 항목 추가.
   - 없으면 **맨 위(최신)** 에 새 `## YYYY-MM-DD` 섹션 생성 후 항목 추가(내림차순 유지).
6. **간결·사실 위주.** 상세가 길면 `troubleshooting/`(이슈별 파일)에 두고 이력엔 요약 + 해당 이슈 파일 링크.
7. ⚠️ **민감정보(비밀번호·DB 계정/접속 IP 등) 기록 금지.** 필요시 "(접속정보는 context.xml 참조)" 식 우회.
8. 저장 후 추가 항목을 1줄로 요약 보고.

## 파일 헤더(신규 생성 시)
```
# 작업 이력 — <yyyy-MM> · <작성자>

> 시간순(최신 위). 항목: `### [분류] 제목` + 증상/원인/조치/변경 파일/검증/관련.
> 안내/규칙: [../작업이력.md](../작업이력.md) · 주제별 상세: [../troubleshooting/](../troubleshooting/README.md)

---
```

## 비고
- 이 스킬은 **내용 생성** 담당. **자동 유도**는 Stop 훅(`.claude/hooks/worklog-nudge.ps1`)이 "변경 있는데 미기록 → /worklog 권고"로 경고만(비차단).
- 산출물·스킬·훅 모두 `.claude/` 아래라 SVN 팀 공유. **작성자별 파일**이라 동시 작업해도 충돌 안 난다.
- 전체를 한눈에 보려면 `worklog/` 디렉터리(파일명 정렬). 합쳐 보고 싶으면 읽기 전용 조합 스크립트 사용(커밋 X).
