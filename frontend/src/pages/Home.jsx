import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'
import styles from './Home.module.css'

export default function Home() {
  const { user, logout, deleteAccount } = useAuth()
  const navigate = useNavigate()
  
  // 상태 변수들
  const [step, setStep] = useState('checking') // checking, input, loading, result
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [loadingTextIndex, setLoadingTextIndex] = useState(0)
  const [apiError, setApiError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  // 사주 입력 폼 상태
  const [form, setForm] = useState({
    name: '',
    gender: 'MALE',
    calendarType: 'SOLAR',
    birthYear: '1995',
    birthMonth: '5',
    birthDay: '5',
    birthHour: '12',
    birthMinute: '00',
    noTime: false,
    birthPlace: '서울'
  })

  // 백엔드에서 받아온 사주 및 캐릭터 정보
  const [sajuResult, setSajuResult] = useState(null)

  // 신비로운 로딩 문구들
  const loadingTexts = [
    "🌠 천체의 궤도와 별자리의 배열을 추적하는 중...",
    "☯️ 생년월일의 오행(목, 화, 토, 금, 수) 균형을 연산하는 중...",
    "📜 명리학의 만세력 데이터를 분석하여 운명의 실타래를 짜는 중...",
    "🛡️ 당신의 운명에 조화로운 영웅의 클래스를 각성하는 중...",
    "⚔️ 모험을 위한 운명의 캐릭터 시트를 완성하고 있습니다!"
  ]

  // 로딩 단계 텍스트 롤링 효과
  useEffect(() => {
    let interval;
    if (step === 'loading') {
      interval = setInterval(() => {
        setLoadingTextIndex((prev) => (prev + 1) % loadingTexts.length)
      }, 2500)
    } else {
      setLoadingTextIndex(0)
    }
    return () => clearInterval(interval)
  }, [step])

  // 기존에 저장된 사용자의 사주 데이터가 있는지 조회
  useEffect(() => {
    const fetchSajuInfo = async () => {
      try {
        const response = await api.get('/saju/me')
        if (response.status === 200 && response.data) {
          setSajuResult(response.data)
          setStep('result')
        } else {
          setStep('input')
        }
      } catch (err) {
        // 데이터가 없거나(204 No Content) 에러인 경우 입력 폼 표시
        setStep('input')
      }
    }
    fetchSajuInfo()
  }, [])

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  const handleDeleteAccount = async () => {
    await deleteAccount()
    navigate('/login')
  }

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target
    setForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setApiError('')
    setStep('loading')
    setIsSubmitting(true)

    // 데이터 가공
    const birthTime = form.noTime 
      ? null 
      : `${form.birthHour.padStart(2, '0')}:${form.birthMinute.padStart(2, '0')}`

    const payload = {
      name: form.name,
      gender: form.gender,
      calendarType: form.calendarType,
      birthYear: parseInt(form.birthYear),
      birthMonth: parseInt(form.birthMonth),
      birthDay: parseInt(form.birthDay),
      birthTime: birthTime,
      birthPlace: form.birthPlace
    }

    // 최소 5초간 로딩 신비로움을 유지한 후 결과 표시 (UX 가치 극대화)
    const startTime = Date.now()

    try {
      const response = await api.post('/saju', payload)
      const elapsedTime = Date.now() - startTime
      const remainingTime = Math.max(0, 5000 - elapsedTime)

      setTimeout(() => {
        setSajuResult(response.data)
        setStep('result')
        setIsSubmitting(false)
      }, remainingTime)

    } catch (err) {
      setStep('input')
      setIsSubmitting(false)
      setApiError(err.response?.data?.message || '사주 분석 요청 중 오류가 발생했습니다.')
    }
  }

  // 1900 ~ 2030 연도 배열 생성
  const years = Array.from({ length: 131 }, (_, i) => 2030 - i)
  const months = Array.from({ length: 12 }, (_, i) => i + 1)
  const days = Array.from({ length: 31 }, (_, i) => i + 1)
  const hours = Array.from({ length: 24 }, (_, i) => i)
  const minutes = Array.from({ length: 60 }, (_, i) => i)

  const cities = [
    "서울", "경기", "인천", "강원", "충북", "충남", "대전", "세종",
    "전북", "전남", "광주", "경북", "경남", "대구", "울산", "부산", "제주", "해외"
  ]

  // 오행 속성에 부합하는 AI 연성 도트 그래픽 매핑
  const getCharacterImage = (element) => {
    if (sajuResult && sajuResult.imageData) {
      return sajuResult.imageData;
    }
    if (!element) return '/pixel_gold_knight.png';
    if (element.includes('Fire')) return '/pixel_fire_mage.png';
    if (element.includes('Metal')) return '/pixel_gold_knight.png';
    if (element.includes('Water')) return '/pixel_fire_mage.png'; // 대체 자원
    return '/pixel_gold_knight.png'; // 기본값
  };

  // 각 속성에 해당하는 정사각형 스킬 아이콘 매칭
  const getSkillImage = (element) => {
    if (!element) return '/pixel_skill_blue.png';
    if (element.includes('Fire')) return '/pixel_skill_fire.png';
    if (element.includes('Metal')) return '/pixel_skill_blue.png';
    if (element.includes('Water')) return '/pixel_skill_fire.png'; // 대체 자원
    return '/pixel_skill_blue.png'; // 기본값
  };

  return (
    <div className={styles.container}>
      <nav className={styles.nav}>
        <span className={styles.logo}>⚔️ DestinyCode</span>
        <div className={styles.navRight}>
          <span className={styles.nickname}>{user?.nickname} 님</span>
          <button onClick={handleLogout} className={styles.btnOutline}>
            로그아웃
          </button>
        </div>
      </nav>

      <main className={styles.main}>
        {/* 1단계: 사용자 정보 조회 중 */}
        {step === 'checking' && (
          <div className={styles.checkingContainer}>
            <div className={styles.spinner}></div>
            <p>운명의 기록을 조회하고 있습니다...</p>
          </div>
        )}

        {/* 2단계: 사주 정보 입력 폼 */}
        {step === 'input' && (
          <div className={styles.inputCard}>
            <h2 className={styles.heading}>✨ 나의 운명 캐릭터 생성</h2>
            <p className={styles.desc}>
              당신의 사주 정보를 만세력 기준으로 입력하세요. <br />
              음양오행의 조화를 분석하여 고유의 RPG 캐릭터를 강림시킵니다.
            </p>

            <form onSubmit={handleSubmit} className={styles.form}>
              {/* 이름 */}
              <div className={styles.formGroup}>
                <label className={styles.label}>이름 (Name)</label>
                <input
                  type="text"
                  name="name"
                  value={form.name}
                  onChange={handleChange}
                  placeholder="모험가의 이름을 알려주세요"
                  className={styles.input}
                  required
                />
              </div>

              {/* 성별 선택 */}
              <div className={styles.formGroup}>
                <label className={styles.label}>성별 (Gender)</label>
                <div className={styles.tabContainer}>
                  <label className={`${styles.tabItem} ${form.gender === 'MALE' ? styles.tabActive : ''}`}>
                    <input
                      type="radio"
                      name="gender"
                      value="MALE"
                      checked={form.gender === 'MALE'}
                      onChange={handleChange}
                      className={styles.radioHidden}
                    />
                    남성 ♂️
                  </label>
                  <label className={`${styles.tabItem} ${form.gender === 'FEMALE' ? styles.tabActive : ''}`}>
                    <input
                      type="radio"
                      name="gender"
                      value="FEMALE"
                      checked={form.gender === 'FEMALE'}
                      onChange={handleChange}
                      className={styles.radioHidden}
                    />
                    여성 ♀️
                  </label>
                </div>
              </div>

              {/* 달력 기준 (음양력) */}
              <div className={styles.formGroup}>
                <label className={styles.label}>달력 기준 (Calendar Type)</label>
                <div className={styles.tabContainer}>
                  <label className={`${styles.tabItem} ${form.calendarType === 'SOLAR' ? styles.tabActive : ''}`}>
                    <input
                      type="radio"
                      name="calendarType"
                      value="SOLAR"
                      checked={form.calendarType === 'SOLAR'}
                      onChange={handleChange}
                      className={styles.radioHidden}
                    />
                    양력 (Solar)
                  </label>
                  <label className={`${styles.tabItem} ${form.calendarType === 'LUNAR_PLAIN' ? styles.tabActive : ''}`}>
                    <input
                      type="radio"
                      name="calendarType"
                      value="LUNAR_PLAIN"
                      checked={form.calendarType === 'LUNAR_PLAIN'}
                      onChange={handleChange}
                      className={styles.radioHidden}
                    />
                    음력 평달
                  </label>
                  <label className={`${styles.tabItem} ${form.calendarType === 'LUNAR_LEAP' ? styles.tabActive : ''}`}>
                    <input
                      type="radio"
                      name="calendarType"
                      value="LUNAR_LEAP"
                      checked={form.calendarType === 'LUNAR_LEAP'}
                      onChange={handleChange}
                      className={styles.radioHidden}
                    />
                    음력 윤달
                  </label>
                </div>
              </div>

              {/* 생년월일 */}
              <div className={styles.formGroup}>
                <label className={styles.label}>생년월일 (Birth Date)</label>
                <div className={styles.inputGrid}>
                  <select name="birthYear" value={form.birthYear} onChange={handleChange} className={styles.select}>
                    {years.map((y) => (
                      <option key={y} value={y}>{y}년</option>
                    ))}
                  </select>
                  <select name="birthMonth" value={form.birthMonth} onChange={handleChange} className={styles.select}>
                    {months.map((m) => (
                      <option key={m} value={m}>{m}월</option>
                    ))}
                  </select>
                  <select name="birthDay" value={form.birthDay} onChange={handleChange} className={styles.select}>
                    {days.map((d) => (
                      <option key={d} value={d}>{d}일</option>
                    ))}
                  </select>
                </div>
              </div>

              {/* 태어난 시간 */}
              <div className={styles.formGroup}>
                <div className={styles.labelWithAction}>
                  <label className={styles.label}>태어난 시간 (Birth Time)</label>
                  <label className={styles.checkboxLabel}>
                    <input
                      type="checkbox"
                      name="noTime"
                      checked={form.noTime}
                      onChange={handleChange}
                      className={styles.checkbox}
                    />
                    시간 모름 ❓
                  </label>
                </div>
                {!form.noTime && (
                  <div className={styles.inputGrid2}>
                    <select name="birthHour" value={form.birthHour} onChange={handleChange} className={styles.select}>
                      {hours.map((h) => (
                        <option key={h} value={h}>{String(h).padStart(2, '0')}시</option>
                      ))}
                    </select>
                    <select name="birthMinute" value={form.birthMinute} onChange={handleChange} className={styles.select}>
                      {minutes.map((m) => (
                        <option key={m} value={m}>{String(m).padStart(2, '0')}분</option>
                      ))}
                    </select>
                  </div>
                )}
              </div>

              {/* 태어난 장소 */}
              <div className={styles.formGroup}>
                <label className={styles.label}>태어난 장소 (Birth Place)</label>
                <select name="birthPlace" value={form.birthPlace} onChange={handleChange} className={styles.selectFull}>
                  {cities.map((city) => (
                    <option key={city} value={city}>{city}</option>
                  ))}
                </select>
              </div>

              {apiError && <p className={styles.errorText}>⚠️ {apiError}</p>}

              <button type="submit" className={styles.btnSubmit}>
                🔮 운명의 캐릭터 강림시키기
              </button>
            </form>
          </div>
        )}

        {/* 3단계: 신비로운 우주적 로딩 화면 */}
        {step === 'loading' && (
          <div className={styles.loadingContainer}>
            <div className={styles.magicPortal}>
              <div className={styles.magicOrb}></div>
              <div className={styles.magicRing}></div>
              <div className={styles.magicRing2}></div>
            </div>
            <div className={styles.loadingTextContainer}>
              <p className={styles.loadingMessage}>{loadingTexts[loadingTextIndex]}</p>
              <div className={styles.progressBar}>
                <div className={styles.progressFill} style={{ width: `${(loadingTextIndex + 1) * 20}%` }}></div>
              </div>
            </div>
          </div>
        )}

        {/* 4단계: 캐릭터 정보 생성 성공 결과 */}
        {step === 'result' && sajuResult && (
          <div className={styles.resultContainer}>
            <div className={styles.resultHeader}>
              <span className={styles.badge}>{sajuResult.characterSummary.element} 속성</span>
              <h2 className={styles.characterTitle}>{sajuResult.characterSummary.title}</h2>
              <h1 className={styles.characterName}>{sajuResult.characterSummary.className} "{sajuResult.name}"</h1>
            </div>

            <div className={styles.cardBody}>
              {/* AI 연성 도트 캐릭터 비주얼 카드 */}
              <div className={styles.characterVisualCard}>
                <div className={styles.pixelFrame}>
                  <img 
                    src={getCharacterImage(sajuResult.characterSummary.element)} 
                    alt="AI 연성 도트 영웅" 
                    className={styles.characterImage}
                  />
                </div>
                <div className={styles.visualMeta}>
                  <span className={styles.visualSparkle}>✨</span>
                  <span className={styles.visualText}>AI 운명 연성 16비트 도트 캐릭터 외형</span>
                  <span className={styles.visualSparkle}>✨</span>
                </div>
              </div>

              {/* 캐릭터 세부 정보 시트 */}
              <div className={styles.sajuSheet}>
                <h3 className={styles.sectionTitle}>📜 명리(命理) 대운 데이터</h3>
                <div className={styles.sheetGrid}>
                  <div className={styles.sheetItem}>
                    <span className={styles.sheetLabel}>성명</span>
                    <span className={styles.sheetValue}>{sajuResult.name} ({sajuResult.gender === 'MALE' ? '남' : '여'})</span>
                  </div>
                  <div className={styles.sheetItem}>
                    <span className={styles.sheetLabel}>달력</span>
                    <span className={styles.sheetValue}>
                      {sajuResult.calendarType === 'SOLAR' ? '양력' : sajuResult.calendarType === 'LUNAR_PLAIN' ? '음력 평달' : '음력 윤달'}
                    </span>
                  </div>
                  <div className={styles.sheetItem}>
                    <span className={styles.sheetLabel}>생년월일</span>
                    <span className={styles.sheetValue}>{sajuResult.birthYear}년 {sajuResult.birthMonth}월 {sajuResult.birthDay}일</span>
                  </div>
                  <div className={styles.sheetItem}>
                    <span className={styles.sheetLabel}>태어난 시</span>
                    <span className={styles.sheetValue}>{sajuResult.birthTime ? sajuResult.birthTime : '시간 장막(모름)'}</span>
                  </div>
                  <div className={styles.sheetItem}>
                    <span className={styles.sheetLabel}>기운 서린 곳</span>
                    <span className={styles.sheetValue}>{sajuResult.birthPlace}</span>
                  </div>
                </div>
              </div>

              {/* 운명 분석 및 스토리 설명 */}
              <div className={styles.characterStory}>
                <h3 className={styles.sectionTitle}>🛡️ 모험가 운명서 (Destiny Story)</h3>
                <p className={styles.storyText}>{sajuResult.characterSummary.description}</p>
              </div>

              {/* 캐릭터 스탯 시각화 (오행 기반 재미로 보는 기운 수치) */}
              <div className={styles.statsContainer}>
                <h3 className={styles.sectionTitle}>⚡ 영혼의 기본 능력치 (Destiny Stats)</h3>
                <div className={styles.statList}>
                  <div className={styles.statItem}>
                    <div className={styles.statInfo}>
                      <span>체력 (Earth)</span>
                      <span>{sajuResult.characterSummary.element.includes('Earth') ? '92' : sajuResult.characterSummary.element.includes('Metal') ? '80' : '65'}</span>
                    </div>
                    <div className={styles.statBarContainer}>
                      <div 
                        className={styles.statBarFill} 
                        style={{ width: sajuResult.characterSummary.element.includes('Earth') ? '92%' : sajuResult.characterSummary.element.includes('Metal') ? '80%' : '65%' }}
                      ></div>
                    </div>
                  </div>

                  <div className={styles.statItem}>
                    <div className={styles.statInfo}>
                      <span>공격력 (Fire)</span>
                      <span>{sajuResult.characterSummary.element.includes('Fire') ? '95' : sajuResult.characterSummary.element.includes('Wood') ? '78' : '68'}</span>
                    </div>
                    <div className={styles.statBarContainer}>
                      <div 
                        className={styles.statBarFill} 
                        style={{ width: sajuResult.characterSummary.element.includes('Fire') ? '95%' : sajuResult.characterSummary.element.includes('Wood') ? '78%' : '68%', backgroundColor: '#ff4d4d' }}
                      ></div>
                    </div>
                  </div>

                  <div className={styles.statItem}>
                    <div className={styles.statInfo}>
                      <span>마력 (Water)</span>
                      <span>{sajuResult.characterSummary.element.includes('Water') ? '98' : sajuResult.characterSummary.element.includes('Fire') ? '82' : '70'}</span>
                    </div>
                    <div className={styles.statBarContainer}>
                      <div 
                        className={styles.statBarFill} 
                        style={{ width: sajuResult.characterSummary.element.includes('Water') ? '98%' : sajuResult.characterSummary.element.includes('Fire') ? '82%' : '70%', backgroundColor: '#00d2fc' }}
                      ></div>
                    </div>
                  </div>
                </div>
              </div>

              {/* 신령 전용 비술 스킬 슬롯 세트 */}
              <div className={styles.skillContainer}>
                <h3 className={styles.sectionTitle}>🔮 신내림 전용 비술 (Shamanic Skills)</h3>
                <div className={styles.skillList}>
                  <div className={styles.skillSlot}>
                    <div className={styles.skillIconFrame}>
                      <img 
                        src={getSkillImage(sajuResult.characterSummary.element)} 
                        alt="주력 비술" 
                        className={styles.skillIcon}
                      />
                    </div>
                    <span className={styles.skillName}>
                      {sajuResult.characterSummary.element.includes('Fire') ? '지옥 화무(火舞)' : '저승 철검술'}
                    </span>
                    <span className={styles.skillType}>액티브 비술</span>
                  </div>
                  <div className={`${styles.skillSlot} ${styles.locked}`}>
                    <div className={styles.skillIconFrame}>
                      <div className={styles.lockOverlay}>🔒</div>
                    </div>
                    <span className={styles.skillName}>잠겨진 기운</span>
                    <span className={styles.skillType}>봉인됨</span>
                  </div>
                  <div className={`${styles.skillSlot} ${styles.locked}`}>
                    <div className={styles.skillIconFrame}>
                      <div className={styles.lockOverlay}>🔒</div>
                    </div>
                    <span className={styles.skillName}>미확인 비술</span>
                    <span className={styles.skillType}>봉인됨</span>
                  </div>
                </div>
              </div>
            </div>

            <div className={styles.actionButtons}>
              <button onClick={() => setStep('input')} className={styles.btnOutline}>
                🔄 사주 다시 입력하기
              </button>
            </div>
          </div>
        )}

        {/* 위험 구역: 회원 탈퇴 */}
        {step !== 'loading' && (
          <div className={styles.danger}>
            <button
              onClick={() => setShowDeleteConfirm(true)}
              className={styles.btnDanger}
            >
              회원 탈퇴
            </button>
          </div>
        )}
      </main>

      {/* 탈퇴 모달 */}
      {showDeleteConfirm && (
        <div className={styles.overlay}>
          <div className={styles.modal}>
            <h3>정말 탈퇴하시겠습니까?</h3>
            <p>탈퇴 시 모든 데이터가 삭제되며 복구할 수 없습니다.</p>
            <div className={styles.modalButtons}>
              <button onClick={() => setShowDeleteConfirm(false)} className={styles.btnOutline}>
                취소
              </button>
              <button onClick={handleDeleteAccount} className={styles.btnDanger}>
                탈퇴하기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
