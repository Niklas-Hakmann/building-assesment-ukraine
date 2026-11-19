import { useEffect, useState } from 'react';
import { BuildingPage, BuildingsOverview, LoadingState } from './features/buildings';
import { useBuildingStore, useSensorStore } from './store';
import './styles.css';

/** "#/buildings/3" → 3; anything else → the overview. Hash routing needs no server support. */
function useBuildingRoute(): number | null {
  const parse = () => {
    const match = window.location.hash.match(/^#\/buildings\/(\d+)$/);
    return match ? Number(match[1]) : null;
  };
  const [buildingId, setBuildingId] = useState(parse);
  useEffect(() => {
    const onChange = () => {
      setBuildingId(parse());
      window.scrollTo(0, 0);
    };
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);
  return buildingId;
}

function App() {
  const { sensors, connection, now } = useSensorStore();
  const { buildings, error } = useBuildingStore();
  const buildingId = useBuildingRoute();

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <h1>
            <a href="#/" className="home-link">
              Building Assessment Ukraine
            </a>
          </h1>
          <p className="page__subtitle">Structural sensor dashboard</p>
        </div>
      </header>

      {/* Wait for both sources, so a building never flashes "No data" before its first readings arrive. */}
      {buildings === null || connection !== 'live' ? (
        <LoadingState buildingsError={error} connection={connection} />
      ) : buildingId === null ? (
        <BuildingsOverview buildings={buildings} sensors={sensors} now={now} />
      ) : (
        <BuildingPage buildings={buildings} sensors={sensors} now={now} buildingId={buildingId} />
      )}
    </div>
  );
}

export default App;
