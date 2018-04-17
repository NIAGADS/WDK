import React from 'react';

import Header from 'Ui/Header';

import Mesa from 'Mesa/Ui/Mesa';
import TableData from 'Content/TableData';
import CustomSetup from 'Content/CustomSetup';
import ProductionEmulation from 'Content/ProductionEmulation';

const embarrassment = { fontFamily: 'Comic Sans, Comic Sans MS, Papyrus', color: 'blue' };

const ConfigList = [
  {
    label: 'Extra Data + Inline Mode!',
    rows: [
    ...TableData,
    ...TableData,
    ...TableData,
    ...TableData,
    ...TableData,
    ...TableData,
    ...TableData,
    ...TableData,
    ...TableData,
    ...TableData
  ],
    options: {
      title: '10x Regular Data!',
      inline: true,
      paginate: true,
      inlineMaxWidth: '500px',
      columnDefaults: {
        truncated: false,
        overflowHeight: '2rem'
      }
    }
  },
  {
    label: 'No Data',
    columns: null,
    rows: [],
    options: null
  },
  {
    label: 'Production Emulation',
    columns: ProductionEmulation,
    rows: TableData,
    options: {
      title: 'Data Sets'
    }
  },
  {
    label: 'Custom Setup with Filter',
    columns: CustomSetup,
    rows: TableData,
    options: {
      title: <span style={embarrassment}>Cool Internet ! (｡◕‿◕｡)</span>
    }
  },
  {
    label: 'No Configuration / Auto Mode',
    rows: TableData
  }
];

class Root extends React.Component {
  constructor (props) {
    super(props);
    let [ initial, ...others ] = ConfigList;
    this.state = { config: initial };

    this.changeConfig = this.changeConfig.bind(this);
    this.renderConfigMenu = this.renderConfigMenu.bind(this)
  }

  changeConfig (config) {
    this.setState({ config });
  }

  renderConfigMenu () {
    const active = this.state.config;
    const list = ConfigList.map(config => {
      const isActive = config === active;
      return (
        <box
          key={config.label}
          onClick={() => this.changeConfig(config)}
          className={'ConfigMenu-Item' + (isActive ? ' active' : '')}
        >
          {config.label}
        </box>
      );
    });

    return <stack className="ConfigMenu">{list}</stack>
  }

  render () {
    const { config } = this.state;
    return (
      <row className="Root">
        <aside>
          <Header />
          {this.renderConfigMenu()}
        </aside>
        <main>
          <Mesa
            rows={config.rows}
            options={config.options}
            columns={config.columns}
          />
        </main>
      </row>
    );
  }
};

export default Root;
