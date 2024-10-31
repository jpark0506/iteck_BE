// 시간 데이터 로드 버튼 클릭 시 이벤트 처리
document.getElementById('loadTimeDataBtn').addEventListener('click', function() {
    loadData('time');
});

// 사이클 데이터 로드 버튼 클릭 시 이벤트 처리
document.getElementById('loadCycleDataBtn').addEventListener('click', function() {
    loadData('cycle');
});

// 데이터 로드 함수
// 데이터 로드 함수
function loadData(type) {
    const fixedFactor = document.getElementById('fixedFactor').value;
    if (!fixedFactor) {
        alert('Fixed Factor를 입력하세요.');
        return;
    }

    // API 엔드포인트 설정
    const apiUrl = `http://localhost:8080/exp/import/${type}?fixed=${encodeURIComponent(fixedFactor)}`;
    console.log("Requesting URL:", apiUrl); // URL 로그 추가

    // API 호출
    fetch(apiUrl, { method: "GET" })
        .then(response => {
            if (!response.ok) {
                throw new Error('데이터를 가져오는 데 실패했습니다.');
            }
            return response.json();
        })
        .then(apiResponse => {
            plotExperimentData(apiResponse); // 차트 그리기
        })
        .catch(error => {
            console.error('오류 발생:', error);
            alert('데이터 로드 중 오류가 발생했습니다.');
        });
}


// 드롭다운에서 선택한 요소에 따라 그래프를 시각화하는 함수
function plotExperimentData(apiResponse) {
    const chunkDtoList = apiResponse.data; // List of ChunkDto

    // 그래프 타입 선택에 따른 X, Y축 설정
    const graphType = document.getElementById('graphType').value; // 그래프 타입 선택

    let selectedXAxisType;
    let selectedYAxisType;

    // 그래프 타입에 따라 x축과 y축 설정
    switch (graphType) {
        case '시간-전압 그래프':
            selectedXAxisType = 'Time'; // 시간[s]
            selectedYAxisType = 'Voltage(V)'; // 전압[V]
            break;
        case '시간-전류 그래프':
            selectedXAxisType = 'Time'; // 시간[s]
            selectedYAxisType = 'Current(mA)'; // 전류[A]
            break;
        case '시간-dQ/dV 그래프':
            selectedXAxisType = 'Time'; // 시간[s]
            selectedYAxisType = 'dQm/dV(mAh/V_g)'; // dQ/dV[mAh/V]
            break;
        case '사이클-용량 그래프':
            selectedXAxisType = 'Cycle Index'; // 사이클 횟수[회]
            selectedYAxisType = 'Chg_ Spec_ Cap_(mAh/g)'; // 용량[mAh/g or mAh]
            break;
        case '사이클-쿨롱효율 그래프':
            selectedXAxisType = 'Cycle Index'; // 사이클 횟수[회]
            selectedYAxisType = 'Coulombic Efficiency(%)'; // 쿨롱효율[%]
            break;
        default:
            console.error('유효하지 않은 그래프 타입');
            return;
    }

    // 여러 그래프를 그리기 위해 그래프를 담을 컨테이너를 동적으로 생성
    const graphContainer = document.getElementById('graphContainer');
    graphContainer.innerHTML = ''; // 기존 차트를 제거

    chunkDtoList.forEach((chunkDto) => {
        const meta = chunkDto.meta;
        const chunksWrapper = chunkDto.chunks; // { "chunks-실험ID": [...] }
        const chunkKeys = Object.keys(chunksWrapper);

        chunkKeys.forEach(chunkKey => {
            const chunks = chunksWrapper[chunkKey];

            chunks.forEach(chunk => {
                const rowData = chunk.rowData;

                // 데이터 포인트 생성
                const dataPoints = rowData.map(row => {
                    const xValue = parseFloat(row[selectedXAxisType]); // x축에 사용할 값
                    const yValue = parseFloat(row[selectedYAxisType]); // y축에 사용할 값

                    // 유효성 검사
                    if (isNaN(xValue) || isNaN(yValue)) {
                        console.warn('유효하지 않은 데이터 포인트:', row);
                        return null; // 유효하지 않은 포인트는 무시
                    }

                    return { x: xValue, y: yValue };
                }).filter(point => point !== null); // 유효한 포인트만 남김

                // 데이터셋에 추가
                if (dataPoints.length > 0) {
                    // 그래프를 위한 새로운 캔버스를 동적으로 추가
                    const canvas = document.createElement('canvas');
                    canvas.id = `chart-${chunkKey}`;
                    canvas.style.width = '80%';
                    canvas.style.height = '400px';
                    graphContainer.appendChild(canvas);

                    // 차트 생성
                    const ctx = canvas.getContext('2d');
                    new Chart(ctx, {
                        type: 'line',
                        data: {
                            datasets: [{
                                label: `${meta?.title || 'Unknown'} (${chunkKey})`,
                                data: dataPoints,
                                borderColor: getRandomColor(),
                                fill: false,
                                tension: 0.1
                            }]
                        },
                        options: {
                            scales: {
                                x: {
                                    type: 'linear',
                                    position: 'bottom',
                                    title: {
                                        display: true,
                                        text: selectedXAxisType // 선택한 x축 이름을 제목으로 설정
                                    }
                                },
                                y: {
                                    title: {
                                        display: true,
                                        text: selectedYAxisType // 선택한 y축 이름을 제목으로 설정
                                    }
                                }
                            }
                        }
                    });
                } else {
                    console.warn('유효한 데이터 포인트가 없습니다.');
                }
            });
        });
    });
}

// 랜덤 색상 생성 함수
function getRandomColor() {
    const letters = '0123456789ABCDEF';
    let color = '#';
    for (let i = 0; i < 6; i++) {
        color += letters[Math.floor(Math.random() * 16)];
    }
    return color;
}
