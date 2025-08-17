rootProject.name = "orielle-project"

// 공통(frontend, backend) 모듈
include("utils")

// 백엔드 서브 모듈 빌드 스크립트
include("backend:orielle-idessy-service")
include("backend:orielle-friend-service")